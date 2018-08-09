package com.odysseusinc.arachne.executionengine.service.impl;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.KerberosAuthMechanism.KEYTAB;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.utils.TemplateUtils;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.service.KerberosService;
import com.odysseusinc.arachne.executionengine.service.impl.RuntimeServiceImpl.RuntimeServiceMode;
import com.odysseusinc.arachne.executionengine.util.CommandBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KerberosServiceImpl implements KerberosService {

    private static final Logger log = LoggerFactory.getLogger(KerberosService.class);
    private static final String LOG_FILE = "kinit_out.txt";
    private static final String RUNTIME_ENV_KRB_KEYTAB = "KRB_KEYTAB";
    private static final String RUNTIME_ENV_KRB_CONF = "KRB_CONF";
    private static final String RUNTIME_ENV_RUN_KINIT = "RUN_KINIT";
    private static final String KRB_KEYTAB_PATH = "/etc/krb.keytab";
    private static final String KINIT_COMMAND = "kinit";

    @Value("${kerberos.timeout}")
    private long timeout;

    @Value("${kerberos.kinitPath}")
    private String kinitPath;

    @Value("${kerberos.configPath}")
    private String configPath;

    private final static String REALMS = "[realms]";
    private final ThreadLocal<Path> keytab = new ThreadLocal<>();
    private final ThreadLocal<Path> tempConfigPath = new ThreadLocal<>();

    @Override
    public Pair<Map<String, String>, String[]> prepareToKinit(DataSourceUnsecuredDTO dataSource, File workDir, RuntimeServiceMode environmentMode) throws IOException {

        Map<String, String> krbEnvProps = new HashMap<>();
        String[] command = new String[]{};
        if (dataSource.getUseKerberos()) {
            command = buildKinitCommand(dataSource);
            if (RuntimeServiceMode.SINGLE == environmentMode) {
                addKrbRealmToConfig(dataSource, false);
            } else {
                addKrbRealmToConfig(dataSource, true);
                fillKrbEnvProps(command, dataSource, krbEnvProps);
            }
        }
        return new Pair<>(krbEnvProps, command);
    }

    public List<Path> getTempFilePaths() {

        List<Path> paths = new ArrayList<>();

        if (Objects.nonNull(keytab.get())) {
            paths.add(keytab.get());
        }
        if (Objects.nonNull(tempConfigPath.get())) {
            paths.add(tempConfigPath.get());
        }
        return paths;
    }

    private void fillKrbEnvProps(String[] command, DataSourceUnsecuredDTO dataSource, Map<String, String> krbEnvProps) {

        String commandStr = StringUtils.join(command, " ");
        commandStr = commandStr.replace(kinitPath + KINIT_COMMAND, "");
        if (KEYTAB == dataSource.getKrbAuthMethod()) {
            String tempKeytabName = keytab.get().toString();
            commandStr = commandStr.replace(tempKeytabName, KRB_KEYTAB_PATH);
            krbEnvProps.put(RUNTIME_ENV_KRB_KEYTAB, tempKeytabName);
        }
        krbEnvProps.put(RUNTIME_ENV_KRB_CONF, tempConfigPath.get().toString());
        krbEnvProps.put(RUNTIME_ENV_RUN_KINIT, commandStr);
    }

    private String[] buildKinitCommand(DataSourceUnsecuredDTO dataSource) throws IOException {

        CommandBuilder builder = CommandBuilder.newCommand();
        if (StringUtils.isBlank(dataSource.getKrbUser())) {
            throw new IllegalArgumentException("Kerberos user is required for authentication");
        }
        switch (dataSource.getKrbAuthMethod()) {
            case PASSWORD:
                if (StringUtils.isBlank(dataSource.getKrbPassword())) {
                    throw new IllegalArgumentException("Kerberos password is required for PASSWORD authentication");
                }
                builder.statement("bash")
                        .withParam("-c")
                        .statement("echo " + dataSource.getKrbPassword() + " | " + kinitPath + KINIT_COMMAND +
                                dataSource.getKrbUser() + "@" + dataSource.getKrbRealm());
                break;
            case KEYTAB:
                if (Objects.isNull(dataSource.getKrbKeytab())) {
                    throw new IllegalArgumentException("Kerberos keytab file is required for KEYTAB authentication");
                }
                keytab.set(Files.createTempFile("", ".keytab"));
                try (OutputStream out = new FileOutputStream(keytab.get().toFile())) {
                    IOUtils.write(dataSource.getKrbKeytab(), out);
                }
                builder.statement(kinitPath + KINIT_COMMAND)
                        .withParam("-k")
                        .withParam("-t")
                        .withParam(keytab.get().toString())
                        .withParam(dataSource.getKrbUser() + "@" + dataSource.getKrbRealm());
                break;
            default:
                throw new IllegalArgumentException("Unsupported authentication type");
        }
        String[] command = builder.build();
        if (log.isDebugEnabled()) {
            log.debug("Kerberos init command: {}", StringUtils.join(command, " "));
        }
        return command;
    }

    public void runKinit(File workDir, String[] command) throws IOException {

        File stdout = new File(workDir, LOG_FILE);
        ProcessBuilder pb = new ProcessBuilder();
        Process process = pb.directory(workDir)
                .redirectOutput(ProcessBuilder.Redirect.to(stdout))
                .redirectError(ProcessBuilder.Redirect.appendTo(stdout))
                .command(command).start();
        try {
            process.waitFor(timeout, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                log.warn("kinit exit code: {}", process.exitValue());
            }
            process.destroy();
            if (log.isDebugEnabled()) {
                klist(workDir);
            }
        } catch (InterruptedException e) {
            log.error("Failed to obtain kerberos ticket", e);
        }
        List<Path> paths = getTempFilePaths();
        for (Path path : paths) {
            FileUtils.deleteQuietly(path.toFile());
        }
    }

    private synchronized void addKrbRealmToConfig(DataSourceUnsecuredDTO dataSource, boolean isInIsolatedMode) throws IOException {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("realmName", dataSource.getKrbRealm());
        parameters.put("adminServer", dataSource.getKrbFQDN());
        parameters.put("kdcServer", dataSource.getKrbFQDN());

        Template confTemplate = TemplateUtils.loadTemplate("templates/krb5Conf.mustache");
        String textString = confTemplate.apply(parameters);

        File config = new File(configPath);
        if (isInIsolatedMode) {
            tempConfigPath.set(Files.createTempFile("", ".conf"));
            File tempConfFile = tempConfigPath.get().toFile();
            FileUtils.copyFile(config, tempConfFile);
            config = tempConfFile;
        }
        long fileLength = config.length();
        try (RandomAccessFile raf = new RandomAccessFile(config, "rw")) {
            String confStr = convertConfigToString(raf, (int) fileLength);
            boolean isRealmDefined = confStr.toLowerCase().contains(" " + dataSource.getKrbRealm().toLowerCase() + " = {");
            if (!isRealmDefined) {
                int startPosition = confStr.indexOf(REALMS) + REALMS.length();
                String confWithNewRealm = confStr.replace(confStr.substring(0, startPosition + 1), confStr.substring(0, startPosition + 1) + textString);
                raf.seek(0);
                raf.write(confWithNewRealm.getBytes());
            }
        }
    }

    private String convertConfigToString(RandomAccessFile raf, int fileLength) throws IOException {

        raf.seek(0);
        byte[] fileBytes = new byte[fileLength];
        raf.readFully(fileBytes);
        return new String(fileBytes);
    }

    private void klist(File workDir) {

        File stdout = new File(workDir, LOG_FILE);
        try {
            Process process = new ProcessBuilder()
                    .directory(workDir)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(stdout))
                    .redirectError(ProcessBuilder.Redirect.appendTo(stdout))
                    .command("klist")
                    .start();
            process.waitFor(timeout, TimeUnit.SECONDS);
            process.destroy();
        } catch (IOException | InterruptedException ignored) {
        }
    }
}

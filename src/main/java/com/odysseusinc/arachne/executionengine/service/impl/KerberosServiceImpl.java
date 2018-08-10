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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
    private static final List<String> tempFileNames = Arrays.asList(RUNTIME_ENV_KRB_CONF, RUNTIME_ENV_KRB_KEYTAB);
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

    @Override
    public Pair<Map<String, String>, String[]> prepareToKinit(DataSourceUnsecuredDTO dataSource, File workDir, RuntimeServiceMode environmentMode) throws IOException {

        Map<String, String> krbEnvProps = new HashMap<>();
        Pair<String[], List<Path>> commandTmpPaths = buildKinitCommand(dataSource);
        if (RuntimeServiceMode.SINGLE == environmentMode) {
            addKrbRealmToConfig(dataSource, false);
        } else {
            Path tempConfigPath = addKrbRealmToConfig(dataSource, true);
            commandTmpPaths.getRight().add(tempConfigPath);
            fillKrbEnvProps(commandTmpPaths, dataSource, krbEnvProps);
        }
        return Pair.of(krbEnvProps, commandTmpPaths.getLeft());
    }

    @Override
    public List<String> getTempFileNames() {

        return tempFileNames;
    }

    private void fillKrbEnvProps(Pair<String[], List<Path>> commandTmpPaths, DataSourceUnsecuredDTO dataSource, Map<String, String> krbEnvProps) {

        String commandStr = StringUtils.join(commandTmpPaths.getLeft(), " ");
        commandStr = commandStr.replace(kinitPath + KINIT_COMMAND, "");
        String tempConfigPath = commandTmpPaths.getRight().stream().filter(p -> p.toString().contains(".conf")).findFirst().orElseGet(() -> Paths.get("")).toString();
        if (KEYTAB == dataSource.getKrbAuthMethod()) {
            String tempKeytabName = commandTmpPaths.getRight().stream().filter(p -> p.toString().contains(".keytab")).findFirst().orElseGet(() -> Paths.get("")).toString();
            commandStr = commandStr.replace(tempKeytabName, KRB_KEYTAB_PATH);
            krbEnvProps.put(RUNTIME_ENV_KRB_KEYTAB, tempKeytabName);
        }
        krbEnvProps.put(RUNTIME_ENV_KRB_CONF, tempConfigPath);
        krbEnvProps.put(RUNTIME_ENV_RUN_KINIT, commandStr);
    }

    private Pair<String[], List<Path>> buildKinitCommand(DataSourceUnsecuredDTO dataSource) throws IOException {

        CommandBuilder builder = CommandBuilder.newCommand();
        if (StringUtils.isBlank(dataSource.getKrbUser())) {
            throw new IllegalArgumentException("Kerberos user is required for authentication");
        }
        Path keytab = Paths.get("");
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
                keytab = Files.createTempFile("", ".keytab");
                try (OutputStream out = new FileOutputStream(keytab.toFile())) {
                    IOUtils.write(dataSource.getKrbKeytab(), out);
                }
                builder.statement(kinitPath + KINIT_COMMAND)
                        .withParam("-k")
                        .withParam("-t")
                        .withParam(keytab.toString())
                        .withParam(dataSource.getKrbUser() + "@" + dataSource.getKrbRealm());
                break;
            default:
                throw new IllegalArgumentException("Unsupported authentication type");
        }
        String[] command = builder.build();
        if (log.isDebugEnabled()) {
            log.debug("Kerberos init command: {}", StringUtils.join(command, " "));
        }
        List<Path> paths = new ArrayList<>();
        paths.add(keytab);
        return Pair.of(command, paths);
    }

    public void runKinit(File workDir, String[] command, List<Path> tmpPaths) throws IOException {

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
        for (Path path : tmpPaths) {
            FileUtils.deleteQuietly(path.toFile());
        }
    }

    private synchronized Path addKrbRealmToConfig(DataSourceUnsecuredDTO dataSource, boolean isInIsolatedMode) throws IOException {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("realmName", dataSource.getKrbRealm());
        parameters.put("adminServer", dataSource.getKrbAdminFQDN());
        parameters.put("kdcServer", dataSource.getKrbFQDN());

        Template confTemplate = TemplateUtils.loadTemplate("templates/krb5Conf.mustache");
        String textString = confTemplate.apply(parameters);

        Path tempConfigPath = Paths.get("");
        File config = new File(configPath);
        if (isInIsolatedMode) {
            tempConfigPath = Files.createTempFile("", ".conf");
            File tempConfFile = tempConfigPath.toFile();
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
        return tempConfigPath;
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

package com.odysseusinc.arachne.executionengine.service.impl;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.KerberosAuthMechanism.KEYTAB;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.utils.TemplateUtils;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.KerberosAuthMethod;
import com.odysseusinc.arachne.executionengine.model.KrbConfig;
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
import java.util.Collections;
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
    private static final String KINIT_COMMAND = "kinit";

    @Value("${kerberos.timeout}")
    private long timeout;

    @Value("${kerberos.kinitPath}")
    private String kinitPath;

    @Value("${kerberos.configPath}")
    private String configPath;

    private final static String REALMS = "[realms]";

    @Override
    public synchronized KrbConfig prepareToKinit(DataSourceUnsecuredDTO dataSource, RuntimeServiceMode environmentMode) throws IOException {

        KrbConfig krbConfig = new KrbConfig();

        Path configPath;
        if (RuntimeServiceMode.ISOLATED == environmentMode) {
            configPath = buildTempKrbConf(dataSource);
        } else {
            configPath = extendKrbConf(Paths.get(this.configPath), dataSource);
        }
        krbConfig.setConfPath(configPath);

        Path keytabPath = null;
        if (dataSource.getKrbAuthMethod().equals(KerberosAuthMethod.KEYTAB)) {
            if (Objects.isNull(dataSource.getKrbKeytab())) {
                throw new IllegalArgumentException("Kerberos keytab file is required for KEYTAB authentication");
            }
            keytabPath = Files.createTempFile("", ".keytab");
            try (OutputStream out = new FileOutputStream(keytabPath.toFile())) {
                IOUtils.write(dataSource.getKrbKeytab(), out);
            }
            krbConfig.setKeytabPath(keytabPath);
        }

        String[] kinitCommand = buildKinitCommand(dataSource, keytabPath);
        krbConfig.setKinitCommand(kinitCommand);

        return krbConfig;
    }

    @Override
    public List<String> getTempFileNames() {

        return tempFileNames;
    }

    public void runKinit(File workDir, KrbConfig krbConfig) throws IOException {

        File stdout = new File(workDir, LOG_FILE);
        ProcessBuilder pb = new ProcessBuilder();
        Process process = pb.directory(workDir)
                .redirectOutput(ProcessBuilder.Redirect.to(stdout))
                .redirectError(ProcessBuilder.Redirect.appendTo(stdout))
                .command(krbConfig.getKinitCommand()).start();
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

        FileUtils.deleteQuietly(krbConfig.getKeytabPath().toFile());
    }

    private String[] buildKinitCommand(DataSourceUnsecuredDTO dataSource, Path keytab) throws IOException {

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
                if (Objects.isNull(keytab)) {
                    throw new IllegalArgumentException("Kerberos keytab file is required for KEYTAB authentication");
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
        return command;
    }

    private Path buildTempKrbConf(DataSourceUnsecuredDTO dataSource) throws IOException {

        Path configPath = Files.createTempFile("", ".conf");

        String krbConfHeader = buildKrbConfHeader(dataSource.getKrbRealm());

        FileUtils.write(configPath.toFile(), krbConfHeader, UTF_8);
        extendKrbConf(configPath, dataSource);

        return configPath;
    }

    private Path extendKrbConf(Path configPath, DataSourceUnsecuredDTO dataSource) throws IOException {

        File config = configPath.toFile();

        String krbConfEntry = buildKrbConfEntry(dataSource);

        long fileLength = config.length();
        try (RandomAccessFile raf = new RandomAccessFile(config, "rw")) {
            String confStr = convertConfigToString(raf, (int) fileLength);
            boolean isRealmDefined = confStr.toLowerCase().contains(" " + dataSource.getKrbRealm().toLowerCase() + " = {");
            if (!isRealmDefined) {
                int startPosition = confStr.indexOf(REALMS) + REALMS.length();
                String confWithNewRealm = confStr.replace(confStr.substring(0, startPosition + 1), confStr.substring(0, startPosition + 1) + krbConfEntry);
                raf.seek(0);
                raf.write(confWithNewRealm.getBytes());
            }
        }

        return configPath;
    }

    private String buildKrbConfHeader(String defaultRealmName) throws IOException {

        return TemplateUtils
                .loadTemplate("templates/krb5ConfHeader.mustache")
                .apply(Collections.singletonMap("defaultRealmName", defaultRealmName));
    }

    private String buildKrbConfEntry(DataSourceUnsecuredDTO dataSource) throws IOException {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("realmName", dataSource.getKrbRealm());
        parameters.put("adminServer", dataSource.getKrbAdminFQDN());
        parameters.put("kdcServer", dataSource.getKrbFQDN());

        Template confTemplate = TemplateUtils.loadTemplate("templates/krb5ConfEntry.mustache");
        return confTemplate.apply(parameters);
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

package com.odysseusinc.arachne.executionengine.service.impl;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
    public static final String LOG_FILE = "kinit_out.txt";
    private Map<String, String> krbEnvProps = new HashMap<>();

    @Value("${kerberos.timeout}")
    private long timeout;

    @Value("${kerberos.kinitPath}")
    private String kinitPath;

    @Value("${kerberos.configPath}")
    private String configPath;

    private final static String REALMS = "[realms]";

    public void kinit(DataSourceUnsecuredDTO dataSource, File workDir, RuntimeServiceMode environmentMode) throws IOException {

        if (dataSource.getUseKerberos()) {
            String[] command;
            Path keytab = null;
            addKrbRealmToConfig(dataSource);
            try {
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
                                .statement("echo " + dataSource.getKrbPassword() + " | " + kinitPath + "kinit" +
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
                        builder.statement(kinitPath + "kinit")
                                .withParam("-k")
                                .withParam("-t")
                                .withParam(keytab.toString())
                                .withParam(dataSource.getKrbUser() + "@" + dataSource.getKrbRealm());
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported authentication type");
                }
                command = builder.build();
                if (log.isDebugEnabled()) {
                    log.debug("Kerberos init command: {}", StringUtils.join(command, " "));
                }
                if (environmentMode == RuntimeServiceMode.SINGLE) {
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
                } else {

                    String tempKeytabName = keytab.toString();
                    String commandStr = StringUtils.join(command, " ");
                    commandStr = commandStr.replace(tempKeytabName, "krb.keytab");
                    commandStr = commandStr.replace(kinitPath, "");
                    krbEnvProps.put("KRB_CONF", "/etc/krb5.conf");
                    krbEnvProps.put("KRB_KEYTAB", tempKeytabName);
                    krbEnvProps.put("RUN_KINIT", commandStr);
                }
            } finally {
                if (Objects.nonNull(keytab)) {
                    FileUtils.deleteQuietly(keytab.toFile());
                }
            }
        }
    }

    public Map<String, String> getKrbEnvProps() {

        return Collections.unmodifiableMap(krbEnvProps);
    }

    private synchronized void addKrbRealmToConfig(DataSourceUnsecuredDTO dataSource) throws IOException {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("realmName", dataSource.getKrbRealm());
        parameters.put("adminServer", dataSource.getKrbFQDN());
        parameters.put("kdcServer", dataSource.getKrbFQDN());

        Template confTemplate = TemplateUtils.loadTemplate("templates/krb5Conf.mustache");
        String textString = confTemplate.apply(parameters);

        File config = new File(configPath);
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

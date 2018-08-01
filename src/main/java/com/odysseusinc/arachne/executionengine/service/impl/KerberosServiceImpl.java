package com.odysseusinc.arachne.executionengine.service.impl;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.service.KerberosService;
import com.odysseusinc.arachne.executionengine.util.CommandBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Value("${kerberos.timeout}")
    private long timeout;

    public void kinit(DataSourceUnsecuredDTO dataSource, File workDir) throws IOException {

        if (dataSource.getUseKerberos()) {
            String[] command;
            Path keytab = null;
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
                                .statement("echo " + dataSource.getKrbPassword() + " | kinit " +
                                        dataSource.getKrbUser());
                        break;
                    case KEYTAB:
                        if (Objects.isNull(dataSource.getKrbKeytab())) {
                            throw new IllegalArgumentException("Kerberos keytab file is required for KEYTAB authentication");
                        }
                        keytab = Files.createTempFile("", ".keytab");
                        try (OutputStream out = new FileOutputStream(keytab.toFile())) {
                            IOUtils.write(dataSource.getKrbKeytab(), out);
                        }
                        builder.statement("C:\\Program Files\\MIT\\Kerberos\\bin\\kinit")
                                .withParam("-k")
                                .withParam("-t")
                                .withParam(keytab.toString())
                                .withParam(dataSource.getKrbUser());
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported authentication type");
                }
                command = builder.build();
                if (log.isDebugEnabled()) {
                    log.debug("Kerberos init command: {}", StringUtils.join(command, " "));
                }
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
            }finally {
                if (Objects.nonNull(keytab)) {
                    FileUtils.deleteQuietly(keytab.toFile());
                }
            }
        }
    }

    private void klist(File workDir){
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

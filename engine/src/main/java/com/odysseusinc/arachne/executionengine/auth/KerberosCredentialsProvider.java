package com.odysseusinc.arachne.executionengine.auth;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.datasourcemanager.krblogin.KerberosService;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import com.odysseusinc.datasourcemanager.krblogin.RuntimeServiceMode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Service
@Slf4j
public class KerberosCredentialsProvider implements CredentialsProvider {
    @Autowired
    private KerberosService kerberosService;

    @Override
    public AuthEffects apply(DataSourceUnsecuredDTO ds, Path analysisDir, String analysisDirInContainer) {
        Path keys = analysisDir.resolve("keys");
        File keystoreDir = keys.toFile();

        if (ds.getUseKerberos()) {
            keystoreDir.mkdirs();
            try {
                KrbConfig config = kerberosService.runKinit(ds, RuntimeServiceMode.SINGLE, keystoreDir);
                return new Effects(config, keys);
            } catch (IOException e) {
                log.error("Failed to resolve Kerberos auth for Datasource: {}", ds.getName(), e);
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @AllArgsConstructor
    private static class Effects implements AuthEffects, AuthEffects.AddEnvironmentVariables, AuthEffects.Cleanup {
        private final KrbConfig config;
        private final Path keyFile;

        @Override
        public Map<String, String> getEnvVars() {
            return config.getIsolatedRuntimeEnvs();
        }

        @Override
        public void cleanup() {
            FileUtils.deleteQuietly(keyFile.toFile());
            FileUtils.deleteQuietly(config.getComponents().getKeytabPath().toFile());
            FileUtils.deleteQuietly(config.getConfPath().toFile());
        }
    }
}

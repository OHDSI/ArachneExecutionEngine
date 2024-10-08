package com.odysseusinc.arachne.executionengine.auth;

import com.google.common.collect.ImmutableMap;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@Slf4j
public class BigQueryCredentialsProvider implements CredentialsProvider {
    private static final String RUNTIME_BQ_KEYFILE = "BQ_KEYFILE";

    @Override
    public AuthEffects apply(DataSourceUnsecuredDTO dataSource, Path analysisDir, String analysisDirInContainer) {
        if (dataSource.getType() == DBMSType.BIGQUERY) {
            try {
                Path keyFile = Files.createTempFile(analysisDir, "", ".json").toAbsolutePath();
                try (OutputStream out = Files.newOutputStream(keyFile)) {
                    IOUtils.write(dataSource.getKeyfile(), out);
                }
                Path innerPath = Paths.get(analysisDirInContainer).resolve(keyFile.getFileName());
                return new Effects(dataSource.getConnectionString(), keyFile, innerPath.toString());
            } catch (IOException e) {
                log.error("Failed to resolve BigQuery authentication for Source: [{}]", dataSource.getName(), e);
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @AllArgsConstructor
    private static class Effects implements AuthEffects, AuthEffects.AddEnvironmentVariables, AuthEffects.ModifyUrl, AuthEffects.Cleanup {
        private final String newUrl;
        private final Path keyFile;
        private final String keyFileInContainer;

        @Override
        public Map<String, String> getEnvVars() {
            return ImmutableMap.of(RUNTIME_BQ_KEYFILE, keyFileInContainer);
        }

        @Override
        public String getNewUrl() {
            return newUrl + ";OAuthPvtKeyPath=" + keyFileInContainer + ";";
        }

        @Override
        public void cleanup() {
            FileUtils.deleteQuietly(keyFile.toFile());
        }
    }
}

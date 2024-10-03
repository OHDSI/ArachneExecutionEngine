package com.odysseusinc.arachne.executionengine.auth;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;

import java.nio.file.Path;

public interface CredentialsProvider {
    AuthEffects apply(DataSourceUnsecuredDTO dataSource, Path analysisDir, String analysisDirInContainer);
}

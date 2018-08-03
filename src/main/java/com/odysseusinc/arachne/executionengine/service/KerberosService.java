package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.service.impl.RuntimeServiceImpl.RuntimeServiceMode;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface KerberosService {
    void kinit(DataSourceUnsecuredDTO dataSource, File workDir, RuntimeServiceMode environmentMode) throws IOException;

    Map<String, String> getKrbEnvProps();
}

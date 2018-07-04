package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import java.io.File;
import java.io.IOException;

public interface KerberosService {
    void kinit(DataSourceUnsecuredDTO dataSource, File workDir) throws IOException;
}

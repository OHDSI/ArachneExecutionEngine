package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.model.KrbConfig;
import com.odysseusinc.arachne.executionengine.service.impl.RuntimeServiceImpl.RuntimeServiceMode;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface KerberosService {

    KrbConfig prepareToKinit(DataSourceUnsecuredDTO dataSource, RuntimeServiceMode environmentMode) throws IOException;

    void runKinit(File workDir, KrbConfig krbConfig) throws IOException;

    List<String> getTempFileNames();
}

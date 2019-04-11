
package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.model.KrbConfig;
import com.odysseusinc.arachne.executionengine.service.impl.RuntimeServiceImpl.RuntimeServiceMode;
import java.io.File;
import java.io.IOException;

public interface KerberosService {

    KrbConfig runKinit(DataSourceUnsecuredDTO dataSource, RuntimeServiceMode environmentMode, File workDir) throws IOException;
}
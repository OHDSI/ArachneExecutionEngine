package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.service.impl.StdoutHandlerParams;
import com.odysseusinc.arachne.executionengine.util.AnalysisCallback;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;

import java.io.File;
import java.util.concurrent.Future;

public interface DockerService {

    Future analyze(AnalysisSyncRequestDTO analysis, File file, DescriptorBundle descriptorBundle,
                   StdoutHandlerParams stdoutHandlerParams, AnalysisCallback callback, KrbConfig krbConfig);

}

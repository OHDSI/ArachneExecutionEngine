package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.executionengine.auth.AuthEffects;

import java.io.File;
import java.util.function.BiConsumer;

public interface ExecutionService {
     String getExtension();

     Overseer analyze(AnalysisSyncRequestDTO analysis, File dir, BiConsumer<String, String> callback, Integer updateInterval, AuthEffects auth);

}

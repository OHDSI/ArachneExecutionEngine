package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.executionengine.execution.r.ROverseer;
import java.io.File;
import java.util.Optional;
import java.util.function.BiConsumer;

public interface ExecutionService {
     String getExtension();

     AnalysisRequestStatusDTO analyze(AnalysisSyncRequestDTO analysis, File dir, BiConsumer<String, String> callback, Integer updateInterval);

     Optional<ROverseer> getOverseer(Long id);

}

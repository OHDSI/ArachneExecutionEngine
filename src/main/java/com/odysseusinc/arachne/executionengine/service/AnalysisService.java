package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import java.io.File;

public interface AnalysisService {
    AnalysisRequestStatusDTO analyze(AnalysisRequestDTO analysis, File analysisDir, Boolean compressedResult, Long chunkSize);

    int activeTasks();
}

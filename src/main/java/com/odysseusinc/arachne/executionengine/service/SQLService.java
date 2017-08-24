package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import java.io.File;

public interface SQLService {
    void analyze(AnalysisRequestDTO analysis, File file, Boolean compressedResult, Long chunkSize);
}

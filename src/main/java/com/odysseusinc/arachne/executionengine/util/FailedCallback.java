package com.odysseusinc.arachne.executionengine.util;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import java.io.File;

public interface FailedCallback {

    void execute(AnalysisRequestDTO analysis, Throwable e, File resultDir);
}

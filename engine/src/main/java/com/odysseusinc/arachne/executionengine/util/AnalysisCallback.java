package com.odysseusinc.arachne.executionengine.util;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;

import java.io.File;

public interface AnalysisCallback {

    void execute(AnalysisResultStatusDTO status,
                 String stdout,
                 File resultDir,
                 Throwable e);
}

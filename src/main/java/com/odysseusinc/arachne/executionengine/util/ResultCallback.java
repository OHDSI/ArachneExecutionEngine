package com.odysseusinc.arachne.executionengine.util;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import java.io.File;
import java.io.IOException;

public interface ResultCallback {

    void execute(AnalysisRequestDTO analysis,
                 AnalysisResultStatusDTO status,
                 String stdout,
                 File resultDir) throws IOException;
}

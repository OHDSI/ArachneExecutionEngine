package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import java.util.Collection;
import org.springframework.core.io.FileSystemResource;

public interface CallbackService {
    void updateAnalysisStatus(String updateURL, Long submissionId, String out, String password);

    void sendAnalysisResult(String resultURL, String password, AnalysisResultDTO analysisResult,
                            Collection<FileSystemResource> files);
}

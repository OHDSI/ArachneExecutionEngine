package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public interface CdmMetadataService {
    void extractMetadata(final AnalysisRequestDTO analysis, File dir) throws SQLException, IOException;
}

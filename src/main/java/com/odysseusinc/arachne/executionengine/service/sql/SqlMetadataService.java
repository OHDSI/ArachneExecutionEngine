package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.executionengine.model.CdmSource;
import com.odysseusinc.arachne.executionengine.model.Vocabulary;
import java.sql.SQLException;
import java.util.List;

public interface SqlMetadataService {

    boolean tableExists(String tableName) throws SQLException;

    String getCdmVersion() throws SQLException;

    List<CdmSource> getCdmSources() throws SQLException;

    List<Vocabulary> getVocabularyVersions(String cdmVersion) throws SQLException;
}

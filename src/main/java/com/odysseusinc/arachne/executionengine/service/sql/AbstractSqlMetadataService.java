/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: May 12, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.service.sql;

import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.CDM_ETL_REFERENCE;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.CDM_HOLDER;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.CDM_RELEASE_DATE;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.CDM_SOURCE_ABBREVIATION;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.CDM_SOURCE_NAME;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.CDM_VERSION;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.SOURCE_DESCRIPTION;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.SOURCE_DOCUMENTATION_REFERENCE;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.SOURCE_RELEASE_DATE;
import static com.odysseusinc.arachne.executionengine.util.CdmSourceFields.VOCABULARY_VERSION;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.model.CdmSource;
import com.odysseusinc.arachne.executionengine.model.Vocabulary;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

abstract class AbstractSqlMetadataService implements SqlMetadataService {

    private static final String QUERY_VOCABULARY_V4 = "select vocabulary_name from %s.vocabulary";
    private static final String QUERY_VOCABULARY_V5 = "select vocabulary_name, vocabulary_version from %s.vocabulary";
    private static final String REGEX_V4 = "^V4.*";
    private static final String ALL_CDM_QUERY = "select * from %s.cdm_source";
    protected final DataSourceUnsecuredDTO dataSource;
    private RowMapper<Vocabulary> VocabularyVersionRowMapperV5 = (rs) -> {
        String name = rs.getString("vocabulary_name");
        String version = rs.getString("vocabulary_version");
        return new Vocabulary(name, version);
    };
    private RowMapper<Vocabulary> VocabularyVersionRowMapperV4 =
            (rs) -> new Vocabulary(rs.getString("vocabulary_name"), null);

    AbstractSqlMetadataService(DataSourceUnsecuredDTO dataSource) {

        this.dataSource = dataSource;
    }

    <T> T executeQuery(String query, SqlFunction<ResultSet, T> consumer) throws SQLException {

        Objects.requireNonNull(query);
        try (final Connection c = SQLUtils.getConnection(dataSource);
             final PreparedStatement q = c.prepareStatement(query);
             final ResultSet rs = q.executeQuery()) {
            return consumer.apply(rs);
        }
    }

    protected abstract String getDefaultSchema();

    String getSchema() {

        return StringUtils.defaultString(dataSource.getCdmSchema(), getDefaultSchema());
    }

    public List<Vocabulary> getVocabularyVersions(final String cdmVersion) throws SQLException {

        RowMapper<Vocabulary> rowMapper = getVocabularyRowMapper(cdmVersion);
        return executeQuery(getVocabularyQuery(cdmVersion), resultSet -> {
            List<Vocabulary> result = new LinkedList<>();
            while (resultSet.next()) {
                result.add(rowMapper.apply(resultSet));
            }
            return result;
        });
    }

    private String getVocabularyQuery(String cdmVersion) {

        String result;
        if (cdmVersion.matches(REGEX_V4)) {
            result = QUERY_VOCABULARY_V4;
        } else {
            result = QUERY_VOCABULARY_V5;
        }
        result = String.format(result, getSchema());
        return result;
    }

    private RowMapper<Vocabulary> getVocabularyRowMapper(String cdmVersion) {

        RowMapper<Vocabulary> rowMapper;
        if (cdmVersion.matches(REGEX_V4)) {
            rowMapper = VocabularyVersionRowMapperV4;
        } else {
            rowMapper = VocabularyVersionRowMapperV5;
        }
        return rowMapper;
    }

    protected abstract String getCdmQuery();

    public String getCdmVersion() throws SQLException {

        String cdmQuery = String.format(getCdmQuery(), getSchema());
        return executeQuery(cdmQuery, resultSet -> {
            if (resultSet.next()) {
                return resultSet.getString("cdm_version");
            }
            return null;
        });
    }

    @Override
    @FileDescriptorCount
    public boolean tableExists(String tableName) throws SQLException {

        Objects.requireNonNull(tableName);
        String schema = getSchema();
        try (final Connection c = SQLUtils.getConnection(dataSource);
             ResultSet rs = c.getMetaData().getTables(null, schema, tableName, null)) {

            while (rs.next()) {
                if (tableName.equals(rs.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @FileDescriptorCount
    public List<CdmSource> getCdmSources() throws SQLException {

        String query = String.format(ALL_CDM_QUERY, getSchema());
        return executeQuery(query, resultSet -> {
            List<CdmSource> result = new LinkedList<>();
            while (resultSet.next()) {
                CdmSource cdmSource = new CdmSource();
                cdmSource.setName(resultSet.getString(CDM_SOURCE_NAME));
                cdmSource.setAbbreviation(resultSet.getString(CDM_SOURCE_ABBREVIATION));
                cdmSource.setHolder(resultSet.getString(CDM_HOLDER));
                cdmSource.setDescription(resultSet.getString(SOURCE_DESCRIPTION));
                cdmSource.setDocumentationReference(resultSet.getString(SOURCE_DOCUMENTATION_REFERENCE));
                cdmSource.setEtlReference(resultSet.getString(CDM_ETL_REFERENCE));
                cdmSource.setSourceReleaseDate(resultSet.getDate(SOURCE_RELEASE_DATE));
                cdmSource.setCdmReleaseDate(resultSet.getDate(CDM_RELEASE_DATE));
                cdmSource.setCdmVersion(resultSet.getString(CDM_VERSION));
                cdmSource.setVocabularyVersion(resultSet.getString(VOCABULARY_VERSION));
                result.add(cdmSource);
            }
            return result;
        });
    }
}

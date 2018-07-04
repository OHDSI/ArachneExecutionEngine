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

package com.odysseusinc.arachne.executionengine.service.impl;

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
import static com.odysseusinc.arachne.executionengine.util.DateUtil.defaultFormat;
import static org.apache.commons.lang3.StringUtils.defaultString;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonCDMVersionDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.model.CdmSource;
import com.odysseusinc.arachne.executionengine.model.Vocabulary;
import com.odysseusinc.arachne.executionengine.service.CdmMetadataService;
import com.odysseusinc.arachne.executionengine.service.sql.SqlMetadataService;
import com.odysseusinc.arachne.executionengine.service.sql.SqlMetadataServiceFactory;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import com.odysseusinc.arachne.executionengine.util.exception.StatementSQLException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.sql.SqlTranslate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

@Service
public class CdmMetadataServiceImpl implements CdmMetadataService {

    private static final String COMMENT = "CDM database ${database}";
    private static final String PROPERTIES_FILE_NAME = "cdm_version.txt";
    private static final Logger LOGGER = LoggerFactory.getLogger(CdmMetadataService.class);
    private final static String RES_TABLE_CHECK_V4 = "/cdm/v4/tableCheck.sql";
    private final static String RES_TABLE_CHECK_V5 = "/cdm/v5/tableCheck_%s.sql";
    private final static CommonCDMVersionDTO V_5_INIT = CommonCDMVersionDTO.V5_0;
    private final static List<CommonCDMVersionDTO> V5_VERSIONS = new ArrayList<>();
    public static final String VAR_CDM_SCHEMA = "cdm_schema";
    public final static ConcurrentHashMap<Integer, String> detectorSqlMap = new ConcurrentHashMap<>();

    static {
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_3);
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_2);
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_1);
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_0_1);
        V5_VERSIONS.add(V_5_INIT);
    }

    private final SqlMetadataServiceFactory sqlMetadataServiceFactory;
    private final ResourceLoader resourceLoader;
    private final String REGEX_V5 = "^V5.*";

    @Autowired
    public CdmMetadataServiceImpl(SqlMetadataServiceFactory sqlMetadataServiceFactory,
                                  ResourceLoader resourceLoader) {

        this.sqlMetadataServiceFactory = sqlMetadataServiceFactory;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @FileDescriptorCount
    public void extractMetadata(AnalysisRequestDTO analysis, File dir) throws SQLException, IOException {

        DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
        try {
            SqlMetadataService metadataService = sqlMetadataServiceFactory.getMetadataService(dataSource);
            String cdmVersion = detectCdmVersion(dataSource, metadataService);
            List<Vocabulary> vocabularies = metadataService.getVocabularyVersions(cdmVersion);

            Properties properties = new Properties() {
                @Override
                public synchronized Enumeration<Object> keys() {

                    return Collections.enumeration(new TreeSet<>(super.keySet()));
                }
            };
            properties.setProperty("cdm_version", cdmVersion);

            if (cdmVersion.matches(REGEX_V5)) {
                List<CdmSource> cdmSources = metadataService.getCdmSources();
                for (int i = 0; i < cdmSources.size(); i++) {
                    String index = String.format("cdm_source.%03d.", i);
                    CdmSource source = cdmSources.get(i);
                    properties.setProperty(index + CDM_SOURCE_NAME, source.getName());
                    properties.setProperty(index + CDM_SOURCE_ABBREVIATION,
                            defaultString(source.getAbbreviation()));
                    properties.setProperty(index + CDM_HOLDER, defaultString(source.getHolder()));
                    properties.setProperty(index + SOURCE_DESCRIPTION, defaultString(source.getDescription()));
                    properties.setProperty(index + SOURCE_DOCUMENTATION_REFERENCE,
                            defaultString(source.getDocumentationReference()));
                    properties.setProperty(index + CDM_ETL_REFERENCE, defaultString(source.getEtlReference()));
                    properties.setProperty(index + SOURCE_RELEASE_DATE, defaultFormat("%tc", source.getSourceReleaseDate()));
                    properties.setProperty(index + CDM_RELEASE_DATE, defaultFormat("%tc", source.getCdmReleaseDate()));
                    properties.setProperty(index + CDM_VERSION, defaultString(source.getCdmVersion()));
                    properties.setProperty(index + VOCABULARY_VERSION, defaultString(source.getVocabularyVersion()));
                }
            }

            for (int i = 0; i < vocabularies.size(); i++) {
                String index = String.format("vocabulary.%03d.", i);
                Vocabulary vocabulary = vocabularies.get(i);
                properties.setProperty(index + "name", vocabulary.getName());
                if (StringUtils.isNotBlank(vocabulary.getVersion())) {
                    properties.setProperty(index + "version", vocabulary.getVersion());
                }
            }
            File file = new File(dir, PROPERTIES_FILE_NAME);
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file)) {
                @Override
                public void newLine() throws IOException {

                    write("\r\n"); //To make file readable in Windows Notepad
                }
            }) {
                properties.store(bufferedWriter, composeComment(dataSource));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Could not extract CDM metadata", e);
        }
    }

    private String composeComment(DataSourceUnsecuredDTO dataSource) {

        Map<String, String> values = new HashMap<>();
        values.put("database", dataSource.getConnectionString());
        return new StrSubstitutor(values).replace(COMMENT);
    }

    private String detectCdmVersion(DataSourceUnsecuredDTO dataSource, SqlMetadataService metadataService) throws SQLException {

        CommonCDMVersionDTO version = null;
        try {
            for (CommonCDMVersionDTO v : V5_VERSIONS) {
                try {
                    checkCdmTables(dataSource, RES_TABLE_CHECK_V5, v.name());
                    version = v;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Detected CDM version for {} is {}", dataSource.getConnectionStringForLogging(), version);
                    }
                    break;
                } catch (StatementSQLException e) {
                    LOGGER.debug("Failed CDM version check for {} as {} with message: {},\nstatement: {}", dataSource.getConnectionStringForLogging(), v, e.getMessage(), e.getStatement());
                } catch (SQLException e) {
                    LOGGER.debug("Failed CDM version check for {} as {} with message: {}", dataSource.getConnectionStringForLogging(), v, e.getMessage());
                }
            }
            if (Objects.isNull(version)) {
                checkCdmTables(dataSource, RES_TABLE_CHECK_V4, "");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to determine CDM version", e);
            version = null;
        }
        return Objects.isNull(version) ? null : version.name();
    }

    private void checkCdmTables(DataSourceUnsecuredDTO dataSource, String pattern, String version) throws SQLException, IOException {

        String sql = detectorSqlMap.computeIfAbsent(
                Objects.hash(dataSource.getType().getOhdsiDB(), pattern, version),
                (key) -> {
                    Resource queryFile = resourceLoader.getResource(ResourceUtils.CLASSPATH_URL_PREFIX + String.format(pattern, version));
                    try (Reader r = new InputStreamReader(queryFile.getInputStream())) {
                        return SqlTranslate.translateSql(IOUtils.toString(r), dataSource.getType().getOhdsiDB());
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
        );

        String[] params = new String[]{VAR_CDM_SCHEMA};
        String[] values = new String[]{dataSource.getCdmSchema()};
        sql = SqlRender.renderSql(sql, params, values);

        String[] statements = SqlSplit.splitSql(sql);

        try (Connection c = SQLUtils.getConnection(dataSource)) {
            for (String query : statements) {
                if (StringUtils.isNotBlank(query)) {
                    try (PreparedStatement stmt = c.prepareStatement(query)) {
                        stmt.setMaxRows(1);
                        try (final ResultSet resultSet = stmt.executeQuery()) {
                        }
                    } catch (SQLException e) {
                        throw new StatementSQLException(e.getMessage(), e, query);
                    }
                }
            }
        }
    }

}

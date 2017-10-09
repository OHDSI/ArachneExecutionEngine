/**
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
 * Created: August 24, 2017
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

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;
import com.odysseusinc.arachne.executionengine.model.CdmSource;
import com.odysseusinc.arachne.executionengine.model.Vocabulary;
import com.odysseusinc.arachne.executionengine.service.CdmMetadataService;
import com.odysseusinc.arachne.executionengine.service.sql.SqlMetadataService;
import com.odysseusinc.arachne.executionengine.service.sql.SqlMetadataServiceFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdmMetadataServiceImpl implements CdmMetadataService {

    private static final String COMMENT = "CDM database ${database}";
    private static final String PROPERTIES_FILE_NAME = "cdm_version.txt";
    private final SqlMetadataServiceFactory sqlMetadataServiceFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(CdmMetadataService.class);
    private final String REGEX_V5 = "^V5.*";

    @Autowired
    public CdmMetadataServiceImpl(SqlMetadataServiceFactory sqlMetadataServiceFactory) {

        this.sqlMetadataServiceFactory = sqlMetadataServiceFactory;
    }

    @Override
    public void extractMetadata(AnalysisRequestDTO analysis, File dir) throws SQLException, IOException {

        DataSourceDTO dataSource = analysis.getDataSource();
        try {
            SqlMetadataService metadataService = sqlMetadataServiceFactory.getMetadataService(dataSource);
            String cdmVersion = detectCdmVersion(metadataService);
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

    private String composeComment(DataSourceDTO dataSource) {

        Map<String, String> values = new HashMap<>();
        values.put("database", dataSource.getConnectionString());
        return new StrSubstitutor(values).replace(COMMENT);
    }

    private String detectCdmVersion(SqlMetadataService metadataService) throws SQLException {

        String version;
        if (metadataService.tableExists("cdm_source")) {
            version = "V5.0";
        } else {
            version = "V4.5";
        }
        return version;
    }

}

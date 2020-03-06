/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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

import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.model.CdmSource;
import com.odysseusinc.arachne.executionengine.model.Vocabulary;
import com.odysseusinc.arachne.executionengine.service.CdmMetadataService;
import com.odysseusinc.arachne.executionengine.service.VersionDetectionServiceFactory;
import com.odysseusinc.arachne.executionengine.service.sql.SqlMetadataService;
import com.odysseusinc.arachne.executionengine.service.sql.SqlMetadataServiceFactory;
import com.odysseusinc.arachne.executionengine.util.DateUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.Callable;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(CdmMetadataService.class);
    private final static CommonCDMVersionDTO V_5_INIT = CommonCDMVersionDTO.V5_0;
    private final static List<CommonCDMVersionDTO> V5_VERSIONS = new ArrayList<>();

    static {
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_3_1);
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_3);
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_2);
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_1);
        V5_VERSIONS.add(CommonCDMVersionDTO.V5_0_1);
        V5_VERSIONS.add(V_5_INIT);
    }

    private final SqlMetadataServiceFactory sqlMetadataServiceFactory;
    private final VersionDetectionServiceFactory versionDetectionServiceFactory;
    private final String REGEX_V5 = "^V(5+|6+)_.*";

    @Autowired
    public CdmMetadataServiceImpl(SqlMetadataServiceFactory sqlMetadataServiceFactory,
                                  VersionDetectionServiceFactory versionDetectionServiceFactory) {

        this.sqlMetadataServiceFactory = sqlMetadataServiceFactory;
        this.versionDetectionServiceFactory = versionDetectionServiceFactory;
    }

    @Override
    @FileDescriptorCount
    public void extractMetadata(DataSourceUnsecuredDTO dataSource, File dir) throws SQLException, IOException {

        try {
            SqlMetadataService metadataService = sqlMetadataServiceFactory.getMetadataService(dataSource);
            String cdmVersion;
            try {
                cdmVersion = logTime(String.format("[%s] CDM Version detection", dataSource.getType()),
                        () -> detectCdmVersion(dataSource));
            } catch (Exception e) {
                LOGGER.error("Failed to detect CDM Version, {}", e.getMessage());
                cdmVersion = "";
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("[%s] CDM version: %s", dataSource.getType(),
                        StringUtils.isNotBlank(cdmVersion) ? cdmVersion : "not detected"));
            }
            List<Vocabulary> vocabularies = Collections.emptyList();
            final String cdm = cdmVersion;
            try {
                vocabularies = logTime(String.format("[%s] vocabulary versions resolving", dataSource.getType()),
                        () -> metadataService.getVocabularyVersions(cdm));
            } catch (Exception e) {
                LOGGER.error("Failed to get metadata, {}", e.getMessage());
            }

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

    private String detectCdmVersion(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Optional<CommonCDMVersionDTO> version;
        try {
            version = Optional.ofNullable(versionDetectionServiceFactory.getService(dataSource.getType())
                    .detectCDMVersion(dataSource));
        } catch (IOException e) {
            LOGGER.error("Failed to determine CDM version", e);
            version = Optional.empty();
        }
        return version.map(CommonCDMVersionDTO::name).orElse("");
    }

    private <V> V logTime(String actionName, Callable<V> statement) throws Exception {

        LocalDateTime start = LocalDateTime.now();
        try{
            return statement.call();
        } finally {
            LocalDateTime finish = LocalDateTime.now();
            Duration timeElapsed = Duration.between(start, finish);
            LOGGER.debug(String.format("Execution of %s took: %s", actionName, DateUtil.formatDuration(timeElapsed)));
        }
    }

}

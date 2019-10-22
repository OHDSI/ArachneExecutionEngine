/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Yaroslav Molodkov, Alexander Cumarav
 * Created: October 17, 2019
 *
 */

package com.odysseusinc.arachne.executionengine.service.impl;

import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.service.VersionDetectionService;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class DefaultVersionDetectionService extends BaseVersionDetectionService implements VersionDetectionService {

    @Override
    public CommonCDMVersionDTO detectCDMVersion(DataSourceUnsecuredDTO dataSource) throws SQLException, IOException {

        Map<String, List<String>> databaseSchema = extractMetadata(dataSource);
        CommonCDMVersionDTO result = doDetectVersion(schema -> isSchemaIncludedBy(schema, databaseSchema));

        // Log warnings around version detection process,
        // might be useful for db masters
        if (Objects.isNull(result) && LOGGER.isDebugEnabled()) {

            LOGGER.debug("CDM version was not detected on datasource: [{}]", dataSource.getName());
            Map<String, List<String>> commonsSchema = parseSchemaJson(COMMONS_SCHEMA);
            if (isSchemaIncludedBy(commonsSchema, databaseSchema)) {
                showCDMCheckWarnings(V5_VERSIONS, databaseSchema, ver -> {
                    try {
                        return parseSchemaJson(String.format(SCHEMA_TMPL, ver.name()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                showCDMCheckWarnings(OTHER_VERSIONS.keySet(), databaseSchema, ver -> {
                    try {
                        return parseSchemaJson(OTHER_VERSIONS.get(ver));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        return result;
    }

    /**
     * Checks if schema1 included into schema2
     * @param schema1
     * @param schema2
     * @return true when schema2 includes schema1
     */
    private boolean isSchemaIncludedBy(Map<String, List<String>> schema1,
                                       Map<String, List<String>> schema2) {

        boolean tablesIncluded = schema2.keySet().containsAll(schema1.keySet());
        return tablesIncluded && schema1.keySet().stream()
                .allMatch(t -> schema2.get(t).containsAll(schema1.get(t)));
    }

    private Map<String, List<String>> extractMetadata(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Map<String, List<String>> metadataMap = new TreeMap<>();
        final String schema = dataSource.getCdmSchema();
        try(Connection c = SQLUtils.getConnection(dataSource)) {
            DatabaseMetaData metaData = c.getMetaData();

            try(ResultSet columns = metaData.getColumns(null, schema, "%", "%")){
                while (columns.next()) {
                    String tableName = columns.getString("TABLE_NAME").toLowerCase();
                    String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                    List<String> tableColumns = metadataMap.getOrDefault(tableName, new ArrayList<>());
                    tableColumns.add(columnName);
                    metadataMap.putIfAbsent(tableName, tableColumns);
                }
            }
        }
        return metadataMap;
    }

    private void showCDMCheckWarnings(Collection<CommonCDMVersionDTO> versions,
                                      Map<String, List<String>> databaseSchema,
                                      Function<CommonCDMVersionDTO, Map<String, List<String>>> schemaFunc) throws IOException {

        for(CommonCDMVersionDTO ver : versions){
            Map<String, List<String>> diff = schemaFunc.apply(ver);
            showSchemaCheckWarnings(diff, databaseSchema, ver);
        }
    }

    private void showSchemaCheckWarnings(Map<String, List<String>> schema,
                                         Map<String, List<String>> databaseSchema,
                                         CommonCDMVersionDTO version) throws IOException {

        List<String> missedTables = diffTables(schema, databaseSchema);
        if (!missedTables.isEmpty()) {
            LOGGER.debug("[{}] Database missed tables: {}", version,
                    String.join(", ", missedTables));
        }
        Map<String, List<String>> missedFields = diffFields(schema, databaseSchema);
        if (!missedFields.isEmpty()) {
            missedFields.keySet().forEach(t -> {
                LOGGER.debug("[{}] Database table {} missed fields: {}", version, t,
                        String.join(", ", missedFields.get(t)));
            });
        }
    }
}

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

package com.odysseusinc.arachne.executionengine.service.versiondetector;

import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.util.SQLUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DefaultVersionDetectionService extends BaseVersionDetectionService implements VersionDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultVersionDetectionService.class);

    @Override
    public Pair<CommonCDMVersionDTO, String> detectCDMVersion(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Map<String, List<String>> databaseSchema = extractMetadata(dataSource);
        return doDetectVersion(databaseSchema, dataSource.getName());
    }

    private Pair<CommonCDMVersionDTO, String> doDetectVersion(Map<String, List<String>> databaseSchema, String datasourceName) {

        Map<String, Map<String, List<String>>> foundDiffs = new LinkedHashMap<>();
        final Map<String, List<String>> v5BaseDiff = calcDifference(COMMONS_SCHEMA, databaseSchema);
        if (v5BaseDiff.isEmpty()) {//V5 base found
            for (CommonCDMVersionDTO version : V5_VERSIONS) {
                final String subVersionColumnsSet = String.format(SCHEMA_TMPL, version.name());
                final Map<String, List<String>> diff = calcDifference(subVersionColumnsSet, databaseSchema);
                if (diff.isEmpty()) {
                    final String optionalColumns = String.format(SCHEMA_TMPL_OPTIONAL, version.name());
                    final Map<String, List<String>> optionalDiff = calcDifference(optionalColumns, databaseSchema);
                    return Pair.of(version, buildOptionalMessage(optionalDiff));
                }
                foundDiffs.put(version.name(), diff);
            }
        } else {
            for (Map.Entry<CommonCDMVersionDTO, String> versionEntry : OTHER_VERSIONS.entrySet()) {
                final Map<String, List<String>> diff = calcDifference(versionEntry.getValue(), databaseSchema);
                if (diff.isEmpty()) {
                    return Pair.of(versionEntry.getKey(), null);
                }
                foundDiffs.put(versionEntry.getKey().name(), diff);
            }
            foundDiffs.put("V5_COMMONS", v5BaseDiff);
        }
        log.debug("CDM version was not detected on datasource: {}", datasourceName);
        for (Map.Entry<String, Map<String, List<String>>> versionEntry : foundDiffs.entrySet()) {
            final Map<String, List<String>> diffs = versionEntry.getValue();
            diffs.forEach((table, columns) -> log.debug("[{}] Database table {}  missed fields: {}", versionEntry.getKey(), table, String.join(", ", columns)));
        }
        return Pair.of(null, toPrettyJSONString(foundDiffs));
    }

    private String toPrettyJSONString(Map<String, ? extends Object> map) {
        JSONObject jsonReport = new JSONObject(map);
        try {
            return jsonReport.toString(4);
        } catch (JSONException e) {
            return jsonReport.toString();
        }
    }

    private Map<String, List<String>> calcDifference(String expectedSchemaResource, Map<String, List<String>> databaseSchema) {

        Map<String, List<String>> expectedSchema = parseSchemaJson(expectedSchemaResource);
        return calculateSchemasDiff(expectedSchema, databaseSchema);
    }

    private Map<String, List<String>> calculateSchemasDiff(Map<String, List<String>> expected,
                                                           Map<String, List<String>> provided) {

        Map<String, List<String>> diff = new HashMap<>();

        final Set<String> tables = expected.keySet();
        for (String table : tables) {
            final List<String> expectedColumns = expected.get(table);
            final List<String> providedColumns = provided.getOrDefault(table, Collections.emptyList());

            final List<String> lostColumns = expectedColumns.stream().filter(name -> !providedColumns.contains(name)).collect(Collectors.toList());
            if (!lostColumns.isEmpty()) {
                diff.put(table, lostColumns);
            }
        }
        return Collections.unmodifiableMap(diff);
    }

    private String buildOptionalMessage(Map<String, List<String>> optionalDiff) {

        if (optionalDiff == null || !optionalDiff.isEmpty()) {
            return toPrettyJSONString(optionalDiff);
        }
        return null;
    }

    private Map<String, List<String>> extractMetadata(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Map<String, List<String>> metadataMap = new TreeMap<>();
        final String schema = dataSource.getCdmSchema();
        try (Connection c = SQLUtils.getConnection(dataSource)) {
            DatabaseMetaData metaData = c.getMetaData();

            try (ResultSet columns = metaData.getColumns(null, schema, "%", "%")) {
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
}

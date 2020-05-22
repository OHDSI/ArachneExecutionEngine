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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.String.join;

@Service
public class DefaultVersionDetectionService extends BaseVersionDetectionService implements VersionDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultVersionDetectionService.class);

    private final CDMSchemaProvider cdmSchemaProvider;
    private final MetadataProvider metadataProvider;

    @Inject
    public DefaultVersionDetectionService(CDMSchemaProvider cdmSchemaProvider, MetadataProvider metadataProvider) {

        this.cdmSchemaProvider = cdmSchemaProvider;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public Pair<CommonCDMVersionDTO, String> detectCDMVersion(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Map<String, List<String>> databaseSchema = metadataProvider.extractMetadata(dataSource);
        return doDetectVersion(databaseSchema, dataSource.getName());
    }

    private Pair<CommonCDMVersionDTO, String> doDetectVersion(Map<String, List<String>> databaseSchema, String datasourceName) {

        Map<String, Map<String, List<String>>> foundDiffs = new TreeMap<>();
        final Map<String, List<String>> expectedCommonCDM = cdmSchemaProvider.loadMandatorySchemaJson(COMMONS_SCHEMA);
        final Map<String, List<String>> v5BaseDiff = calculateSchemasDiff(expectedCommonCDM, databaseSchema);
        if (v5BaseDiff.isEmpty()) {//V5 base found
            for (CommonCDMVersionDTO version : V5_VERSIONS) {
                final String subVersionColumnsResource = buildResourcePath(version);
                final Map<String, List<String>> cmdSubVersionColumnsSet = cdmSchemaProvider.loadMandatorySchemaJson(subVersionColumnsResource);
                final Map<String, List<String>> diff = calculateSchemasDiff(cmdSubVersionColumnsSet, databaseSchema);
                if (diff.isEmpty()) {
                    final Map<String, List<String>> optionalColumnsSet = cdmSchemaProvider.loadOptionalSchemaJson(subVersionColumnsResource);
                    final Map<String, List<String>> optionalDiff = calculateSchemasDiff(optionalColumnsSet, databaseSchema);
                    return Pair.of(version, buildOptionalMessage(version, optionalDiff));
                }
                foundDiffs.put(version.name(), diff);
            }
        } else {
            for (Map.Entry<CommonCDMVersionDTO, String> versionEntry : OTHER_VERSIONS.entrySet()) {
                final Map<String, List<String>> otherVersionColumnsSet = cdmSchemaProvider.loadMandatorySchemaJson(versionEntry.getValue());
                final Map<String, List<String>> diff = calculateSchemasDiff(otherVersionColumnsSet, databaseSchema);
                if (diff.isEmpty()) {
                    return Pair.of(versionEntry.getKey(), null);
                }
                foundDiffs.put(versionEntry.getKey().name(), diff);
            }
            foundDiffs.put("V5_COMMONS", v5BaseDiff);
        }
        final String errorsReport = formatDiffsReport(foundDiffs, true);
        log.debug("CDM version was not detected on datasource: {}", datasourceName);
        log.debug(errorsReport);
        return Pair.of(null, errorsReport);
    }

    private String formatDiffsReport(Map<String, Map<String, List<String>>> foundDiffs, boolean isMandatory) {

        StringBuilder messageBuilder = new StringBuilder();
        String strictnessType = isMandatory ? "mandatory" : "optional";
        for (Map.Entry<String, Map<String, List<String>>> versionEntry : foundDiffs.entrySet()) {
            final Map<String, List<String>> diffs = versionEntry.getValue();
            diffs.forEach((table, columns) -> appendLine(messageBuilder, String.format("[%s] Database table %s  missed %s fields: %s", versionEntry.getKey(), table, strictnessType, join(", ", columns))));
        }
        return messageBuilder.toString();
    }

    private void appendLine(StringBuilder builder, String line) {

        builder.append(line);
        builder.append(System.lineSeparator());
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

    private String buildOptionalMessage(CommonCDMVersionDTO version, Map<String, List<String>> optionalDiff) {

        if (optionalDiff == null || !optionalDiff.isEmpty()) {
            return formatDiffsReport(Collections.singletonMap(version.name(), optionalDiff), false);
        }
        return null;
    }


}

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
 * Created: October 21, 2019
 *
 */

package com.odysseusinc.arachne.executionengine.service.impl;

import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V4_0;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_0;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_0_1;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_1;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_2;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_3;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_3_1;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V6_0;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.executionengine.service.VersionDetectionService;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseVersionDetectionService implements VersionDetectionService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(VersionDetectionService.class);

    protected static final String CDM_V5_RESOURCES = "/cdm/v5/";
    protected static final String SCHEMA_TMPL = CDM_V5_RESOURCES + "diff_%s.json";
    protected static final String COMMONS_SCHEMA = CDM_V5_RESOURCES + "cdm_commons.json";
    protected static final String CDM_V4_SCHEMA = "/cdm/v4/cdm_V4_0.json";
    protected static final String CDM_V6_SCHEMA = "/cdm/v6/cdm_v6_0.json";
    private static final TypeReference<TreeMap<String, ArrayList<String>>> TREE_MAP_TYPE_REFERENCE = new TypeReference<TreeMap<String, ArrayList<String>>>() {};
    protected static Collection<CommonCDMVersionDTO> V5_VERSIONS = new ArrayList<>(6);
    protected static Map<CommonCDMVersionDTO, String> OTHER_VERSIONS = new TreeMap<>();

    static {
        V5_VERSIONS.add(V5_3_1);
        V5_VERSIONS.add(V5_3);
        V5_VERSIONS.add(V5_2);
        V5_VERSIONS.add(V5_1);
        V5_VERSIONS.add(V5_0_1);
        V5_VERSIONS.add(V5_0);
    }

    static {
        OTHER_VERSIONS.put(V6_0, CDM_V6_SCHEMA);
        OTHER_VERSIONS.put(V4_0, CDM_V4_SCHEMA);
    }

    protected CommonCDMVersionDTO doDetectVersion(Predicate<Map<String, List<String>>> schemaPredicate) throws IOException {

        CommonCDMVersionDTO result = null;
        Map<String, List<String>> commonsSchema = parseSchemaJson(COMMONS_SCHEMA);
        if (schemaPredicate.test(commonsSchema)) { //checks is it V5
            for(CommonCDMVersionDTO version : V5_VERSIONS) {
                Map<String, List<String>> diff = parseSchemaJson(String.format(SCHEMA_TMPL, version.name()));
                if (schemaPredicate.test(diff)) {
                    result = version;
                    break;
                }
            }
        } else {
            for(CommonCDMVersionDTO version : OTHER_VERSIONS.keySet()) {
                Map<String, List<String>> cdmSchema = parseSchemaJson(OTHER_VERSIONS.get(version));
                if (schemaPredicate.test(cdmSchema)) {
                    result = version;
                    break;
                }
            }
        }
        return result;
    }

    protected Map<String, List<String>> parseSchemaJson(String resource) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try(Reader reader = new InputStreamReader(getClass().getResourceAsStream(resource))) {
            return mapper.readValue(reader, TREE_MAP_TYPE_REFERENCE);
        }
    }

    protected final List<String> diffTables(Map<String, List<String>> schema1, Map<String, List<String>> schema2) {

        Set<String> schemaTables = schema2.keySet();
        return schema1.keySet().stream()
                .filter(t -> !schemaTables.contains(t))
                .collect(Collectors.toList());
    }

    protected final Map<String, List<String>> diffFields(Map<String, List<String>> schema1, Map<String, List<String>> schema2) {

        BinaryOperator<List<String>> mergeFunction = (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        };
        List<String> tables = schema1.keySet().stream()
                .filter(schema2::containsKey)
                .collect(Collectors.toList());
        return tables.stream().map(t -> {
            List<String> cdmFields = schema1.get(t);
            List<String> dbFields = schema2.get(t);
            List<String> diffFieldsList = cdmFields.stream()
                    .filter(f -> !dbFields.contains(f))
                    .collect(Collectors.toList());
            return Pair.of(t, diffFieldsList);
        })
                .filter(v -> !v.getValue().isEmpty())
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, mergeFunction, TreeMap::new));
    }
}

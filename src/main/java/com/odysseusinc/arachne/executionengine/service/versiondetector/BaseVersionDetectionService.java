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

package com.odysseusinc.arachne.executionengine.service.versiondetector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.executionengine.exceptions.ExecutionEngineRuntimeException;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V4_0;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_0;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_0_1;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_1;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_2;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_3;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V5_3_1;
import static com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO.V6_0;

public abstract class BaseVersionDetectionService implements VersionDetectionService {

    protected static final String CDM_V5_RESOURCES = "/cdm/v5/";
    protected static final String SCHEMA_TMPL = CDM_V5_RESOURCES + "diff_%s.json";
    protected static final String SCHEMA_TMPL_OPTIONAL = CDM_V5_RESOURCES + "optional_diff_%s.json";
    protected static final String COMMONS_SCHEMA = CDM_V5_RESOURCES + "cdm_commons.json";
    protected static final String CDM_V4_SCHEMA = "/cdm/v4/cdm_V4_0.json";
    protected static final String CDM_V6_SCHEMA = "/cdm/v6/cdm_V6_0.json";
    private static final TypeReference<TreeMap<String, ArrayList<String>>> TREE_MAP_TYPE_REFERENCE = new TypeReference<TreeMap<String, ArrayList<String>>>() {
    };
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

    protected Map<String, List<String>> parseSchemaJson(String resource) {

        ObjectMapper mapper = new ObjectMapper();
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(resource))) {
            return mapper.readValue(reader, TREE_MAP_TYPE_REFERENCE);
        } catch (Exception e) {
            throw new ExecutionEngineRuntimeException("Cannot load resource: " + resource, e);
        }
    }
}

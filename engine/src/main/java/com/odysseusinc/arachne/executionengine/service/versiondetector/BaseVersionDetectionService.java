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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public abstract class BaseVersionDetectionService implements VersionDetectionService {

    private static final String CDM_V5_RESOURCES = "/cdm/v5/";
    private static final String SCHEMA_TMPL = CDM_V5_RESOURCES + "diff_%s.json";
    protected static final String COMMONS_SCHEMA = CDM_V5_RESOURCES + "cdm_commons.json";
    protected static final String CDM_V4_SCHEMA = "/cdm/v4/cdm_V4_0.json";
    protected static final String CDM_V6_SCHEMA = "/cdm/v6/cdm_V6_0.json";

    protected static Collection<String> V5_VERSIONS = Arrays.asList(
            "V5_3_1", "V5_3", "V5_2", "V5_1", "V5_0_1", "V5_0"
    );
    protected static Map<String, String> OTHER_VERSIONS = ImmutableMap.of(
            "V6_0", CDM_V6_SCHEMA,
            "V4_0", CDM_V4_SCHEMA
    );

    protected static String buildResourcePath(String version){
        return String.format(SCHEMA_TMPL, version);
    }
}

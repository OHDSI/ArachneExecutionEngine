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
 * Created: April 03, 2019
 *
 */

package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;

public class NetezzaMetadataService extends AbstractSqlMetadataService {

    private static final String CDM_QUERY = "select cdm_version from %s.cdm_source limit 1";

    NetezzaMetadataService(DataSourceUnsecuredDTO dataSource) {
        super(dataSource);
    }

    @Override
    protected String getDefaultSchema() {

        return dataSource.getUsername();
    }

    @Override
    protected String getCdmQuery() {

        return CDM_QUERY;
    }
}

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

package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;

public class SqlServerMetadataService extends AbstractSqlMetadataService implements SqlMetadataService {

    private static final String DEFAULT_SCHEMA = "dbo";
    private static final String CDM_QUERY = "select top 1 cdm_version from %s.cdm_source";

    SqlServerMetadataService(DataSourceUnsecuredDTO dataSource) {

        super(dataSource);
    }

    @Override
    protected String getDefaultSchema() {

        return DEFAULT_SCHEMA;
    }

    @Override
    protected String getCdmQuery() {

        return CDM_QUERY;
    }

}

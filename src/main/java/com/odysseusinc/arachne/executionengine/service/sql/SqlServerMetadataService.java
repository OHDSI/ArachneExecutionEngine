package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;

public class SqlServerMetadataService extends AbstractSqlMetadataService implements SqlMetadataService {

    private static final String DEFAULT_SCHEMA = "dbo";
    private static final String CDM_QUERY = "select top 1 cdm_version from cdm_source";

    SqlServerMetadataService(DataSourceDTO dataSource) {

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

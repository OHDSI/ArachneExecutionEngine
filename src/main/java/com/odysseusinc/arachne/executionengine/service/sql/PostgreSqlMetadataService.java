package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;

public class PostgreSqlMetadataService extends AbstractSqlMetadataService implements SqlMetadataService {

    private static final String CDM_QUERY = "select cdm_version from cdm_source limit 1";
    private static final String DEFAULT_SCHEMA = "public";

    PostgreSqlMetadataService(DataSourceDTO dataSource) {

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

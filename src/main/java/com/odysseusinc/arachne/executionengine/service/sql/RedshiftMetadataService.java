package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;

public class RedshiftMetadataService extends AbstractSqlMetadataService {

    public static final String DEFAULT_SCHEMA = "PUBLIC";
    private static final String CDM_QUERY = "select top 1 cdm_version from cdm_source";

    RedshiftMetadataService(DataSourceDTO dataSource) {

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

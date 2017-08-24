package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;

public class OracleMetadataService extends AbstractSqlMetadataService implements SqlMetadataService {

    private static final String CDM_QUERY = "select cdm_version from cdm_source where ROWNUM = 1";

    OracleMetadataService(DataSourceDTO dataSource) {

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

    @Override
    String getSchema() {

        return super.getSchema().toUpperCase();
    }

}

package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;

public class ImpalaMetadataService extends AbstractSqlMetadataService {

    public static final String DEFAULT_SCHEMA = "default";
    public static final String CDM_QUERY = "select cdm_version from %s.cdm_source limit 1";

    public ImpalaMetadataService(DataSourceUnsecuredDTO dataSource) {

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

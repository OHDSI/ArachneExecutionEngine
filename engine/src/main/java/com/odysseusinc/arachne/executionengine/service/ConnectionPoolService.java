package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import javax.sql.DataSource;

public interface ConnectionPoolService {

    DataSource getDataSource(DataSourceUnsecuredDTO dataSourceDTO);
}

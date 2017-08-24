package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;
import org.springframework.stereotype.Component;

@Component
public class SqlMetadataServiceFactory {

    public SqlMetadataService getMetadataService(DataSourceDTO dataSource) {

        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource is required");
        }
        DBMSType type = dataSource.getType();
        SqlMetadataService result;
        switch (type) {
            case POSTGRESQL:
                result = new PostgreSqlMetadataService(dataSource);
                break;
            case MS_SQL_SERVER:
            case MS_SQL_SERVER_NATIVE:
                result = new SqlServerMetadataService(dataSource);
                break;
            case ORACLE:
                result = new OracleMetadataService(dataSource);
                break;
            case REDSHIFT:
                result = new RedshiftMetadataService(dataSource);
                break;
            default:
                throw new IllegalArgumentException("DBMS " + type + " is not supported");
        }
        return result;
    }
}

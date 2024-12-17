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
 * Created: May 12, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.service.sql;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import org.springframework.stereotype.Component;

@Component
public class SqlMetadataServiceFactory {

    public SqlMetadataService getMetadataService(DataSourceUnsecuredDTO dataSource) {

        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource is required");
        }
        DBMSType type = dataSource.getType();
        SqlMetadataService result;
        switch (type) {
            case SNOWFLAKE:
            case POSTGRESQL:
                result = new PostgreSqlMetadataService(dataSource);
                break;
            case MS_SQL_SERVER:
            case SYNAPSE:
            case PDW:
                result = new SqlServerMetadataService(dataSource);
                break;
            case ORACLE:
                result = new OracleMetadataService(dataSource);
                break;
            case REDSHIFT:
            case IRIS:
                result = new RedshiftMetadataService(dataSource);
                break;
            case IMPALA:
            case BIGQUERY:
            case SPARK:
            case HIVE:
                result = new ImpalaMetadataService(dataSource);
                break;
            case NETEZZA:
                result = new NetezzaMetadataService(dataSource);
                break;
            default:
                throw new IllegalArgumentException("DBMS " + type + " is not supported");
        }
        return result;
    }
}

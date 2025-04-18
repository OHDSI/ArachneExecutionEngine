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

package com.odysseusinc.arachne.executionengine.util;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
public class SQLUtils {

    public static Connection getConnection(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Connection conn = getConnectionWithAutoCommit(dataSource);
        if (!dataSource.getType().equals(DBMSType.BIGQUERY) && !dataSource.getType().equals(DBMSType.SPARK)) {
            conn.setAutoCommit(Boolean.FALSE);
        }

        return conn;
    }

    public static Connection getConnectionWithAutoCommit(DataSourceUnsecuredDTO dataSource) throws SQLException {

        String user = dataSource.getUsername();
        String password = dataSource.getPassword();
        String url = dataSource.getConnectionString();
        log.debug("Using JDBC: " + dataSource.getConnectionStringForLogging());

        Properties info = new Properties();
        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }
        if (dataSource.getType().equals(DBMSType.SNOWFLAKE)) {
            info.put("CLIENT_RESULT_COLUMN_CASE_INSENSITIVE", "true");
            info.put("MULTI_STATEMENT_COUNT", "0");
        }

        // Set the Databricks JDBC driver to mimic Spark's behavior
        if (dataSource.getType().equals(DBMSType.SPARK)) {
            info.put("driver", "com.databricks.client.jdbc.Driver");
            // Replace "spark" with "databricks" in the URL
            url = url.replace("spark", "databricks");
        }

        Connection conn = DriverManager.getConnection(url, info);

        return conn;
    }
}

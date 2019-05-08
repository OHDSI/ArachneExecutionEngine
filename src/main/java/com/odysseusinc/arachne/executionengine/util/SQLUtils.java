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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLUtils {
    private static final Logger logger = LoggerFactory.getLogger(SQLUtils.class);

    public static Connection getConnection(DataSourceUnsecuredDTO dataSource) throws SQLException {

        Connection conn = getConnectionWithAutoCommit(dataSource);
        if (!dataSource.getType().equals(DBMSType.BIGQUERY)) {
            conn.setAutoCommit(Boolean.FALSE);
        }

        return conn;
    }

    public static Connection getConnectionWithAutoCommit(DataSourceUnsecuredDTO dataSource) throws SQLException {

        String user = dataSource.getUsername();
        String password = dataSource.getPassword();
        String url = dataSource.getConnectionString();
        logger.info("Using JDBC: " + dataSource.getConnectionStringForLogging());
        Connection conn = DriverManager.getConnection(url, user, password);

        return conn;
    }
}

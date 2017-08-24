package com.odysseusinc.arachne.executionengine.util;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLUtils {
    private static final Logger logger = LoggerFactory.getLogger(SQLUtils.class);

    public static Connection getConnection(DataSourceDTO dataSource) throws SQLException {

        String user = dataSource.getUsername();
        String password = dataSource.getPassword();
        String url = dataSource.getConnectionString();
        logger.info("Using JDBC: " + url);
        Connection conn = DriverManager.getConnection(url, user, password);
        // conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        conn.setAutoCommit(false);
        return conn;
    }
}

package com.odysseusinc.arachne.executionengine.config;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DriverManagerConfig {

    private static final Logger log = LoggerFactory.getLogger(DriverManagerConfig.class);

    @PostConstruct
    public void driverManagerRedshiftWorkaround() {

        List<String> supportedDrivers = Arrays.asList(
                "org.postgresql.Driver",
                "net.sourceforge.jtds.jdbc.Driver",
                "com.amazon.redshift.jdbc.Driver",
                "com.mysql.jdbc.Driver",
                "com.mysql.fabric.jdbc.FabricMySQLDriver",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "oracle.jdbc.OracleDriver",
                "com.cloudera.impala.jdbc41.Driver"
        );

        java.util.Enumeration<Driver> drivers =  DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(d);
            } catch (SQLException e) {
                throw new RuntimeException("Could not deregister redshift driver");
            }
        }

        supportedDrivers.forEach(driverName -> {
            try {
                Class<?> driverClass = Class.forName(driverName);
                Driver driver = (java.sql.Driver)driverClass.newInstance();
                DriverManager.registerDriver(driver);
                log.info("Loaded JDBC driver: " + driverName);
            } catch (Exception e) {
                log.info("Failed to load JDBC driver: " + driverName);
            }
        });
    }
}

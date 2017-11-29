package com.odysseusinc.arachne.executionengine.config;

import java.sql.Driver;
import java.sql.DriverManager;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DriverManagerConfig {

    private static final Logger log = LoggerFactory.getLogger(DriverManagerConfig.class);

    @PostConstruct
    public void logLoadedDrivers() {

        java.util.Enumeration<Driver> drivers =  DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            log.info("Loaded JDBC driver: " + d.getClass().getName() + ". Version: " + d.getMajorVersion() + "." + d.getMinorVersion());
        }
    }
}

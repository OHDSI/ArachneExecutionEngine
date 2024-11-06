package com.odysseusinc.arachne.executionengine.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.service.ConnectionPoolService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.ohdsi.sql.SqlTranslate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class HikariConnectionPoolService implements ConnectionPoolService {

    private static final String TEST_SQL = "select 1;";

    private Cache<String, DataSource> dataSourceCache;

    @Value("${connectionpool.capacity.min}")
    private int minPoolSize;

    @Value("${connectionpool.capacity.max}")
    private int maxPoolSize;

    @Value("${connectionpool.ttl.minutes}")
    private int ttl;

    @PostConstruct
    public void init() {

        RemovalListener<String, DataSource> removalListener = event -> ((HikariDataSource)event.getValue()).close();
        dataSourceCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ttl, TimeUnit.MINUTES)
                .removalListener(removalListener)
                .build();
    }

    @Override
    public DataSource getDataSource(DataSourceUnsecuredDTO dataSourceDTO) {

        try {
            log.info("Using JDBC: " + dataSourceDTO.getConnectionStringForLogging());
            return dataSourceCache.get(dataSourceDTO.getConnectionStringAndUserAndPassword(),
                    () -> buildDataSource(dataSourceDTO));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private DataSource buildDataSource(DataSourceUnsecuredDTO dataSourceDTO) {

        HikariConfig config = new HikariConfig();
        config.setAutoCommit(true);
        config.setJdbcUrl(dataSourceDTO.getConnectionString());
        config.setUsername(dataSourceDTO.getUsername());
        config.setPassword(dataSourceDTO.getPassword());
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minPoolSize);
        String testSql = SqlTranslate.translateSql(TEST_SQL, dataSourceDTO.getType().getOhdsiDB()).replaceAll(";$", "");
        config.setConnectionTestQuery(testSql);

        return new HikariDataSource(config);
    }
}

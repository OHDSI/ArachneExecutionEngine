package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.commons.types.DBMSType;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DriverLocations {
    @Value("${drivers.location.impala}")
    private String impala;
    @Value("${drivers.location.bq}")
    private String bigQuery;
    @Value("${drivers.location.netezza}")
    private String netezza;
    @Value("${drivers.location.hive}")
    private String hive;
    @Value("${drivers.location.postgresql}")
    private String postgres;
    @Value("${drivers.location.mssql}")
    private String mssql;
    @Value("${drivers.location.redshift}")
    private String redshift;
    @Value("${drivers.location.oracle}")
    private String oracle;
    @Value("${drivers.location.snowflake}")
    private String snowflake;
    @Value("${drivers.location.spark}")
    private String spark;
    @Value("${drivers.location.iris}")
    private String iris;

    public String getPath(DBMSType type) {
        switch (type) {
            case IMPALA:
                return impala;
            case BIGQUERY:
                return bigQuery;
            case NETEZZA:
                return netezza;
            case HIVE:
                return hive;
            case POSTGRESQL:
                return postgres;
            case MS_SQL_SERVER:
            case SYNAPSE:
                return mssql;
            case REDSHIFT:
                return redshift;
            case ORACLE:
                return oracle;
            case SNOWFLAKE:
                return snowflake;
            case SPARK:
                return spark;
            case IRIS:
                return iris;

            default:
                return null;
        }
    }

    public String getPathExclusions() {
        return Stream.of(
                impala, bigQuery, netezza, hive, mssql, postgres, redshift, oracle, snowflake, spark, iris
        ).filter(StringUtils::isNotBlank).map(path ->
                path.startsWith("/") ? path.substring(1) : path
        ).map(path ->
                path + "/**/*"
        ).collect(Collectors.joining(","));

    }
}

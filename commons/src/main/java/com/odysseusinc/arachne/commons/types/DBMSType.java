package com.odysseusinc.arachne.commons.types;

public enum DBMSType {
    // Set of databases both supported by OHDSI/SqlRender and OHDSI/DatabaseConnector
    POSTGRESQL("PostgreSQL", "postgresql"),
    MS_SQL_SERVER("MS SQL Server", "sql server"),
    PDW("SQL Server Parallel Data Warehouse", "pdw"),
    REDSHIFT("Redshift", "redshift"),
    ORACLE("Oracle", "oracle"),
    IMPALA("Impala", "impala"),
    BIGQUERY("Google BigQuery", "bigquery"),
    NETEZZA("Netezza", "netezza"),
    HIVE("Apache Hive", "hive"),
    SPARK("Spark", "spark"),
    IRIS("Iris", "iris"),
    SNOWFLAKE("Snowflake", "snowflake"),
    SYNAPSE("Azure Synapse", "synapse");

    private String label;
    // For further pass into SqlRender.translateSql as "targetDialect" and DatabaseConnector as "dbms"
    private String ohdsiDB;

    DBMSType(String label, String ohdsiDB) {

        this.label = label;
        this.ohdsiDB = ohdsiDB;
    }

    public String getValue() {

        return this.toString();
    }

    public String getLabel() {

        return label;
    }

    public String getOhdsiDB() {

        return ohdsiDB;
    }

    public void setOhdsiDB(String ohdsiDB) {

        this.ohdsiDB = ohdsiDB;
    }
}

# ArachneExecutionEngine
Arachne Execution Engine is a component used to execute remote SQL or R code. It is used by both Arachne Data Node as well as WebAPI

## Build with Impala JDBC driver

1. Download Cloudera JDBC Connector using the following link:
https://www.cloudera.com/downloads/connectors/impala/jdbc/2-5-42.html

1. unzip one with following jars:
   1. hive_metastore.jar
   1. hive_service.jar
   1. ImpalaJDBC41.jar
   1. libfb303-0.9.0.jar
   1. libthrift-0.9.0.jar
   1. ql.jar
   1. TCLIServiceClient.jar
1. Run build with profile **impala**:
    ```:shell 
    mvn -P impala clean install
    ```

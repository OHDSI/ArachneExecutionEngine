# ArachneExecutionEngine
ARACHNE Execution Engine is a component used to execute remote SQL or R code. It is used by both Arachne Data Node as well as WebAPI.

ARACHNE Execution Engine is able to use local or Docker Image/tar ball pre-built R environments to execute your R code.

## Installation
- Option 1. Build from the sources
- Option 2. [Use Docker Image](https://hub.docker.com/r/odysseusinc/execution_engine)

## Configuration

### Sample options for creating a container for running locally

Generic options:

    --rm                                                             // Remove on exit
    -p 8888:8888                                                     // Bind host port to container port
    --add-host=host.docker.internal:host-gateway                     // Allow access to DB running on host bare

For using tarball execution environments:

    -e RUNTIMESERVICE_DIST_ARCHIVE=/dist/r_base_focal_amd64.tar.gz   // Name of the default execution environment  
    -v ~/R-environments:/runtimes                                   // Mount host directory volume 

For using Docker execution environments:

    --privileged                                                     // Allow spawning other containers 
    -v /var/run/docker.sock:/var/run/docker.sock                     // Mount socket to connect to host Docker from inside container 
    -v ~/executions:/etc/executions                                       // Mount host directory /etc/ee to volume /etc/executions in container to hold executions 
    -e DOCKER_ENABLE=true                                            // Enable execution in Docker container                                           
    -e DOCKER_IMAGE_DEFAULT=odysseusinc/r-hades:latest             // Default image to use for running executions 
    -e ANALYSIS_MOUNT=/etc/ee                                        // Provide container location of the host directory for executions to allow mounting it spawn Docker containers
    -e DOCKER_REGISTRY_URL=...                                       // (Optional) url to Docker registry for pulling image runtime files
    -e DOCKER_REGISTRY_USERNAME=...                                  // (Optional) username to connect to Docker registry
    -e DOCKER_REGISTRY_PASSWORD=...                                  // (Optional) password to connect to Docker registry

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

## Running tests

Before running tests it's required to prepare CDM database version 5.0 or newer.
Run tests command should include CDM database connection parameters like shown below:

```bash
mvn -Dcdm.jdbc_url=jdbc:postgresql://localhost/synpuf -Dcdm.username=postgres -Dcdm.password=postgres test
```  

If deployed to DBMS other than PostgreSQL point one 
of the following dbms types with `cdm.dbms` parameter:
- postgresql
- sql server
- pdw
- redshift
- oracle
- implala
- bigquery
- netezza
- snowflake

## Process R files with Docker (locally)

- You can use the following template for testing purposes:

```bash
curl --location 'https://localhost:8888/api/v1/analyze' \
--header 'arachne-compressed: false' \
--header 'arachne-waiting-compressed-result: false' \
--header 'arachne-attach-cdm-metadata: true' \
--header 'arachne-result-chunk-size-mb: 10485760' \
--form 'analysisRequest="{
      \"id\": 123,
      \"executableFileName\": \"main.R\",
      \"dataSource\": {
      \"id\": 123,
      \"name\": \"Data Source\",
      \"url\": \"https://test.com"
      },
      \"requested\": \"2023-12-19T10:00:00Z\",
      \"requestedDescriptorId\": \"789\",
      \"resultExclusions\": \"exclude_result1,exclude_result2\",
      \"dockerImage\": \"r-base\",
      \"callbackPassword\": \"callback-password\",
      \"updateStatusCallback\": \"https://callback-url.com/update\",
      \"resultCallback\": \"https://callback-url.com/result\"
      }";type=application/json' \
--form 'file=@"/Downloads/main.R"' \
--form 'container="r-base"'
```  

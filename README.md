# ArachneExecutionEngine
ARACHNE Execution Engine is a component used to execute remote SQL or R code. It is used by both Arachne Data Node as well as WebAPI.

ARACHNE Execution Engine is able to use local or Docker Image/tar ball pre-built R environments to execute your R code.

## Installation
- Option 1. Build from the sources
- Option 2. [Use Docker Image](https://hub.docker.com/r/odysseusinc/execution_engine)

See the instructuction to deploy the application in combination with [ARACHNE DataNode](https://github.com/OHDSI/Arachne/wiki/Installation-using-Docker)

## Configuration

### Important configuration options

`docker.image.default` - default docker image to be used for execution. If specified, this enables docker execution support.

`docker.image.filter` - a regex used to scan for docker images to be listed. Specifying enables docker execution support and image scanning.

`docker.image.pull` - policy that defines when docker images should be pulled. The possible values are as follows:  
    `NEVER`   Never pull any images. If image does not exist in local repository, fail analysis.  
    `MISSING` If image exists in local repository, use it. Do not check for updated image.  
    `ALWAYS`  Always attempt to pull. If pull failed but image exists in local repository, proceed with that image.   
    `FORCE`   Always attempt to pull. If pull fails, analysis will fail as well, even if local image exists.  

`runtimeservice.dist.archive` - Default image to use for running executions

`runtime.local` - true/false, enables using local R runtime.

`docker.enable` - outdated option. This is only kept for backward compatibility with implementations that don't support dynamic mode detection. 

### Sample options for creating a container for running locally

The configuration below enables both tarball and docker execution:

    --rm                                                             // Remove on exit
    -p 8888:8888                                                     // Bind host port to container port
    --add-host=host.docker.internal:host-gateway                     // Allow access to DB running on host bare
    -e RUNTIMESERVICE_DIST_ARCHIVE=/dist/r_base_focal_amd64.tar.gz   // Name of the default execution environment  
    -v ~/R-environments:/runtimes                                    // Mount host directory volume 
    --privileged                                                     // Allow spawning other containers 
    -v /var/run/docker.sock:/var/run/docker.sock                     // Mount socket to connect to host Docker from inside container 
    -v ~/executions:/etc/executions                                  // Mount host directory /etc/ee to volume /etc/executions in container to hold executions 
    -e DOCKER_IMAGE_DEFAULT=odysseusinc/r-hades:latest               // Default image to use for running executions
    -e DOCKER_IMAGE_FILTER="odysseusinc/r-hades(.+)"                 // Filter to scan for docker images.
    -e ANALYSIS_MOUNT=/etc/ee                                        // Provide container location of the host directory for executions to allow mounting it spawn Docker containers
    -e DOCKER_REGISTRY_URL=...                                       // (Optional) url to Docker registry for pulling image runtime files
    -e DOCKER_REGISTRY_USERNAME=...                                  // (Optional) username to connect to Docker registry
    -e DOCKER_REGISTRY_PASSWORD=...                                  // (Optional) password to connect to Docker registry

### Verifying the configuration

On startup, the list of descriptors and docker images is printed in logs for the ease of verification: 

      2024-09-24 11:38:05.085  INFO : Refreshed TARBALL descriptors (4)
      2024-09-24 11:38:05.086  INFO : TARBALL descriptor [Default] (Default runtime) -> [r_base_focal_amd64.tar.gz]
      2024-09-24 11:38:05.086  INFO : TARBALL descriptor [hades_0.0.1] (Runtime for Hades 1.13.0) -> [r_base_focal_descriptor_hades_1.13.0_amd64.tar.gz]
      2024-09-24 11:38:05.086  INFO : TARBALL descriptor [descriptor_strategus_0.0.6] (Runtime for Strategus 0.0.6) -> [r_base_focal_descriptor_strategus_0.0.6_amd64.tar.gz]
      2024-09-24 11:38:05.487  INFO : Refreshed DOCKER images (4)
      2024-09-24 11:38:05.488  INFO : DOCKER image [null]: [odysseusinc/r-hades:2023q3v3]
      2024-09-24 11:38:05.487  INFO : DOCKER image [sha256:6e00765224ef2388124ab1671491ab6f20a7311f3511dce8d7fbf8d4723f817f]: [odysseusinc/r-hades:2023q3v3, odysseusinc/r-hades:latest]
      2024-09-24 11:38:05.487  INFO : DOCKER image [sha256:2d3be87a6dba17c22b5f713aa312f92af25b9a42bdaae8d3a8d21ba278015c54]: [odysseusinc/r-hades:2023q3v2]
      2024-09-24 11:38:05.488  INFO : DOCKER image [sha256:1d5e30e6cf52d27fabcff618f3d2a61b766ed792f4d585dc2127ac2f3d993699]: [odysseusinc/r-hades:2023q3]

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
- spark

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

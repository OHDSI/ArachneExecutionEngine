package com.odysseusinc.arachne.executionengine.execution.r;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.auth.AuthEffects;
import com.odysseusinc.arachne.executionengine.auth.AuthEffects.AddEnvironmentVariables;
import com.odysseusinc.arachne.executionengine.auth.CredentialsProvider;
import com.odysseusinc.arachne.executionengine.config.properties.HiveBulkLoadProperties;
import com.odysseusinc.arachne.executionengine.execution.DriverLocations;
import com.odysseusinc.arachne.executionengine.execution.FailedOverseer;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.service.ConnectionPoolService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Slf4j
public abstract class RService {
    protected static final String EXECUTION_COMMAND = "Rscript";

    private static final String HOST_DOCKER_INTERNAL = "host.docker.internal";

    private static final String RUNTIME_ENV_DATA_SOURCE_NAME = "DATA_SOURCE_NAME";
    private static final String RUNTIME_ENV_DBMS_USERNAME = "DBMS_USERNAME";
    private static final String RUNTIME_ENV_DBMS_PASSWORD = "DBMS_PASSWORD";
    private static final String RUNTIME_ENV_DBMS_TYPE = "DBMS_TYPE";
    private static final String RUNTIME_ENV_CONNECTION_STRING = "CONNECTION_STRING";
    private static final String RUNTIME_ENV_DBMS_SCHEMA = "DBMS_SCHEMA";
    private static final String RUNTIME_ENV_TARGET_SCHEMA = "TARGET_SCHEMA";
    private static final String RUNTIME_ENV_RESULT_SCHEMA = "RESULT_SCHEMA";
    private static final String RUNTIME_ENV_COHORT_TARGET_TABLE = "COHORT_TARGET_TABLE";
    private static final String RUNTIME_ENV_PATH_KEY = "PATH";
    private static final String RUNTIME_ENV_PATH_VALUE = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";
    private static final String RUNTIME_ENV_HOME_KEY = "HOME";
    private static final String RUNTIME_ENV_HOME_VALUE = "/root";
    private static final String RUNTIME_ENV_HOSTNAME_KEY = "HOSTNAME";
    private static final String RUNTIME_ENV_HOSTNAME_VALUE = "moby";
    private static final String RUNTIME_ENV_LANG_KEY = "LANG";
    private static final String RUNTIME_ENV_LANG_VALUE = "en_US.UTF-8";
    private static final String RUNTIME_ENV_LC_ALL_KEY = "LC_ALL";
    private static final String RUNTIME_ENV_LC_ALL_VALUE = "en_US.UTF-8";
    private static final String RUNTIME_ENV_DRIVER_PATH = "JDBC_DRIVER_PATH";
    @Value("${runtime.killTimeoutSec:30}")
    protected int killTimeoutSec;
    @Value("${runtime.timeOutSec}")
    protected int runtimeTimeOutSec;

    @Value("${bulkload.enableMPP}")
    private boolean enableMPP;
    @Autowired
    private HiveBulkLoadProperties hiveBulkLoadProperties;
    @Autowired
    private ConnectionPoolService poolService;
    @Autowired
    private DriverLocations drivers;
    @Autowired
    private List<CredentialsProvider> credentialProviders;

    private static String sanitizeFilename(String filename) {
        return Objects.requireNonNull(filename).replaceAll("[<>:\"/\\\\|?*\\u0000]", "");
    }

    public Overseer analyze(AnalysisSyncRequestDTO analysis, File analysisDir, BiConsumer<String, String> callback, Integer updateInterval) {
        Long id = analysis.getId();
        DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
        String jdbcUrl = dataSource.getConnectionString();
        if (jdbcUrl.contains(HOST_DOCKER_INTERNAL)) {
            try {
                String address = InetAddress.getByName(HOST_DOCKER_INTERNAL).getHostAddress();
                String newUrl = jdbcUrl.replace(HOST_DOCKER_INTERNAL, address);
                log.info("Resolved {} = [{}]", HOST_DOCKER_INTERNAL, address);
                dataSource.setConnectionString(newUrl);
            } catch (UnknownHostException e) {
                return new FailedOverseer(Instant.now(), "Unable to resolve to [" + HOST_DOCKER_INTERNAL + "]", AnalysisRequestTypeDTO.R, e);
            }
        }

        Path path = analysisDir.toPath();
        AuthEffects auth = credentialProviders.stream().map(provider ->
                provider.apply(dataSource, path, path.toAbsolutePath().toString())
        ).filter(Objects::nonNull).findFirst().orElse(null);

        if (auth instanceof AuthEffects.ModifyUrl) {
            String newUrl = ((AuthEffects.ModifyUrl) auth).getNewUrl();
            dataSource.setConnectionString(newUrl);
        }

        log.info("Execution [{}] checking connection to [{}]", id, dataSource.getConnectionString());
        try (Connection conn = poolService.getDataSource(dataSource).getConnection()) {
            String name = conn.getMetaData().getDatabaseProductName();
            log.info("Execution [{}] connection verified, engine: [{}]", id, name);
        } catch (SQLException | UncheckedExecutionException e) {
            log.info("Execution [{}] connection verification failed [{}]", id, e.getMessage());
        }


        Overseer overseer = analyze(analysis, analysisDir, callback, updateInterval, auth).whenComplete((outcome, throwable) -> {
            if (auth instanceof AuthEffects.Cleanup) {
                ((AuthEffects.Cleanup) auth).cleanup();
            }
        });

        log.info("Execution [{}] started in R Runtime Service", analysis.getId());
        return overseer;
    }

    protected abstract Overseer analyze(AnalysisSyncRequestDTO analysis, File analysisDir, BiConsumer<String, String> callback, Integer updateInterval, AuthEffects auth);

    protected Map<String, String> buildRuntimeEnvVariables(DataSourceUnsecuredDTO dataSource, AuthEffects auth) {
        Map<String, String> environment = new HashMap<>();
        environment.put(RUNTIME_ENV_DATA_SOURCE_NAME, RService.sanitizeFilename(dataSource.getName()));
        environment.put(RUNTIME_ENV_DBMS_USERNAME, dataSource.getUsername());
        environment.put(RUNTIME_ENV_DBMS_PASSWORD, dataSource.getPassword());
        environment.put(RUNTIME_ENV_DBMS_TYPE, dataSource.getType().getOhdsiDB());
        environment.put(RUNTIME_ENV_CONNECTION_STRING, dataSource.getConnectionString());
        environment.put(RUNTIME_ENV_DBMS_SCHEMA, dataSource.getCdmSchema());
        environment.put(RUNTIME_ENV_TARGET_SCHEMA, dataSource.getTargetSchema());
        environment.put(RUNTIME_ENV_RESULT_SCHEMA, dataSource.getResultSchema());
        environment.put(RUNTIME_ENV_COHORT_TARGET_TABLE, dataSource.getCohortTargetTable());
        environment.put(RUNTIME_ENV_DRIVER_PATH, drivers.getPath(dataSource.getType()));
        environment.put(RUNTIME_ENV_PATH_KEY, RUNTIME_ENV_PATH_VALUE);
        environment.put(RUNTIME_ENV_HOME_KEY, getUserHome());
        environment.put(RUNTIME_ENV_HOSTNAME_KEY, RUNTIME_ENV_HOSTNAME_VALUE);
        environment.put(RUNTIME_ENV_LANG_KEY, RUNTIME_ENV_LANG_VALUE);
        environment.put(RUNTIME_ENV_LC_ALL_KEY, RUNTIME_ENV_LC_ALL_VALUE);
        if (auth instanceof AddEnvironmentVariables) {
            environment.putAll(((AddEnvironmentVariables) auth).getEnvVars());
        }

        if (enableMPP) {
            exposeMPPEnvironmentVariables(environment);
        }

        environment.values().removeIf(Objects::isNull);
        return environment;
    }

    private void exposeMPPEnvironmentVariables(Map<String, String> environment) {

        environment.put("USE_MPP_BULK_LOAD", Boolean.toString(enableMPP));
        environment.put("HIVE_NODE_HOST", hiveBulkLoadProperties.getHost());
        environment.put("HIVE_SSH_USER", hiveBulkLoadProperties.getSsh().getUsername());
        environment.put("HIVE_SSH_PORT", Integer.toString(hiveBulkLoadProperties.getSsh().getPort()));
        environment.put("HIVE_SSH_PASSWORD", hiveBulkLoadProperties.getSsh().getPassword());
        if (StringUtils.isNotBlank(hiveBulkLoadProperties.getSsh().getKeyfile())) {
            environment.put("HIVE_KEYFILE", hiveBulkLoadProperties.getSsh().getKeyfile());
        }
        if (StringUtils.isNotBlank(hiveBulkLoadProperties.getHadoop().getUsername())) {
            environment.put("HADOOP_USER_NAME", hiveBulkLoadProperties.getHadoop().getUsername());
        }
        environment.put("HIVE_NODE_PORT", Integer.toString(hiveBulkLoadProperties.getHadoop().getPort()));
    }

    private String getUserHome() {
        String userHome = System.getProperty("user.home");
        return StringUtils.defaultString(userHome, RUNTIME_ENV_HOME_VALUE);
    }

}

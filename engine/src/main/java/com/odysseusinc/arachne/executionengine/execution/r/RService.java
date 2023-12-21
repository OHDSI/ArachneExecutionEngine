/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: March 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.execution.r;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.R;
import static org.apache.commons.io.IOUtils.closeQuietly;

import com.google.common.cache.CacheBuilder;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.util.BigQueryUtils;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.config.properties.HiveBulkLoadProperties;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.execution.DriverLocations;
import com.odysseusinc.arachne.executionengine.execution.ExecutionService;
import com.odysseusinc.arachne.executionengine.execution.KerberosSupport;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependency;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import com.odysseusinc.arachne.executionengine.service.DescriptorService;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import com.odysseusinc.datasourcemanager.krblogin.RuntimeServiceMode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RService implements ExecutionService {
    private static final String EXECUTION_COMMAND = "Rscript";

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
    private static final String RUNTIME_BQ_KEYFILE = "BQ_KEYFILE";
    private static final String RUNTIME_ANALYSIS_ID = "ANALYSIS_ID";

    @Value("${runtime.killTimeoutSec:30}")
    private int killTimeoutSec;
    @Value("${runtime.timeOutSec}")
    private int runtimeTimeOutSec;
    @Value("${bulkload.enableMPP}")
    private boolean enableMPP;

    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private HiveBulkLoadProperties hiveBulkLoadProperties;

    @Autowired
    private RIsolatedRuntimeProperties rIsolatedRuntimeProps;
    @Autowired
    private DescriptorService descriptorService;
    @Autowired
    private KerberosSupport kerberosSupport;

    private final ConcurrentMap<Long, ROverseer> overseers;
    @Autowired
    private DriverLocations drivers;

    @Autowired
    public RService(@Value("${runtime.timeOutSec:259200}") long runtimeTimeout) {
        // Double the runtime timeout to ensure entries stay in map for some time after timeout completion,
        // so that status information is still available
        overseers = CacheBuilder.newBuilder().expireAfterWrite(
                2 * runtimeTimeout, TimeUnit.SECONDS
        ).<Long, ROverseer>build().asMap();
    }

    @PostConstruct
    public void init() {

        if (RuntimeServiceMode.ISOLATED.equals(getRuntimeServiceMode())) {
            log.info("Runtime service running in ISOLATED environment mode");
        } else {
            log.info("Runtime service running in SINGLE mode");
        }
    }

    @Override
    public String getExtension() {
        return "r";
    }


    @Override
    public AnalysisRequestStatusDTO analyze(
            AnalysisSyncRequestDTO analysis, File analysisDir, BiConsumer<String, String> callback, Integer updateInterval
    ) {
        File keystoreDir = new File(analysisDir, "keys");
        KrbConfig krbConfig = kerberosSupport.getConfig(analysis, keystoreDir);

        DescriptorBundle bundle = descriptorService.getDescriptorBundle(analysisDir,
                analysis.getId(), analysis.getRequestedDescriptorId()
        );
        CompletableFuture<ExecutionOutcome> future = analyze(
                analysis, analysisDir, bundle, krbConfig, callback, updateInterval
        ).whenComplete((outcome, throwable) -> {
            // Keystore folder must be deleted before zipping results
            FileUtils.deleteQuietly(keystoreDir);
        });

        String actualDescriptorId = bundle.getDescriptor().getId();
        log.info("Execution [{}] with actual descriptor='{}' started in R Runtime Service", analysis.getId(), actualDescriptorId);
        return new AnalysisRequestStatusDTO(analysis.getId(), R, future, actualDescriptorId);
    }


    @FileDescriptorCount
    public CompletableFuture<ExecutionOutcome> analyze(
            AnalysisSyncRequestDTO analysis, File file, DescriptorBundle descriptorBundle,
            KrbConfig krbConfig, BiConsumer<String, String> callback, Integer updateInterval
    ) {
        Long id = analysis.getId();
        String executableFileName = analysis.getExecutableFileName();
        DataSourceUnsecuredDTO dataSource = analysis.getDataSource();

        try {
            Instant started = Instant.now();
            File jailFile = new File(rIsolatedRuntimeProps.getJailSh());
            boolean externalJail = jailFile.isFile();
            File runFile = externalJail ? jailFile : extractToTempFile(resourceLoader, "classpath:/jail.sh", "ee", ".sh");
            log.info("Execution [{}] initializing jail [{}]", id, runFile.getAbsolutePath());
            prepareEnvironmentInfoFile(file, descriptorBundle);
            prepareRprofile(file);
            String[] command = buildRuntimeCommand(runFile, file, executableFileName, descriptorBundle.getPath());

            final Map<String, String> envp = buildRuntimeEnvVariables(dataSource, krbConfig.getIsolatedRuntimeEnvs());
            envp.put(RUNTIME_ANALYSIS_ID, analysis.getId().toString());

            ProcessBuilder pb = new ProcessBuilder(command).directory(file).redirectErrorStream(true);
            pb.environment().putAll(envp);
            log.info("Execution [{}] start R process: {}", id, command);
            Process process = pb.start();
            ROverseer overseer = new ROverseer(id, process, runtimeTimeOutSec, callback, updateInterval, started, killTimeoutSec);
            overseers.put(id, overseer);
            return overseer.getResult().thenApply(outcome ->
                    // TODO It is weird to see cleanup logic only applied on success, but this is the original behaviour.
                    //      Investigate if it is intentional (e.g. leave environment intact for potential troubleshooting)
                    cleanupEnv(file, outcome)
            ).handle((outcome, throwable) -> {
                if (!externalJail) {
                    FileUtils.deleteQuietly(runFile);
                }
                FileUtils.deleteQuietly(krbConfig.getComponents().getKeytabPath().toFile());
                if (RuntimeServiceMode.ISOLATED == krbConfig.getMode()) {
                    FileUtils.deleteQuietly(krbConfig.getConfPath().toFile());
                }
                return outcome;
            });
        } catch (IOException ex) {
            log.error("Error building runtime command", ex);
            return failedFuture(ex);
        } catch (Throwable t) {
            log.error("Execution [{}] failed to execute in Runtime Service", analysis.getId(), t);
            return failedFuture(t);
        }
    }

    public Optional<ROverseer> getOverseer(Long id) {
        return Optional.ofNullable(overseers.get(id));
    }


    private void prepareEnvironmentInfoFile(File workDir, DescriptorBundle descriptorBundle) {
        Descriptor descriptor = descriptorBundle.getDescriptor();
        final String lineDelimiter = StringUtils.repeat("-", 32);
        try (FileWriter fw = new FileWriter(new File(workDir, "environment.txt")); PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("Analysis Runtime Environment is %s(%s):[%s]\n", descriptor.getBundleName(), descriptor.getLabel(), descriptor.getId());
            if (descriptor.getOsLibraries() != null) {
                pw.println(lineDelimiter);
                pw.println("Operating System Libraries:");
                pw.println(lineDelimiter);
                descriptor.getOsLibraries().forEach(pw::println);
            }
            for (ExecutionRuntime runtime : descriptor.getExecutionRuntimes()) {
                if (runtime instanceof RExecutionRuntime) {
                    pw.println(lineDelimiter);
                    pw.println("R Execution Runtime Libraries:");
                    pw.println(lineDelimiter);
                    RExecutionRuntime rRuntime = (RExecutionRuntime) runtime;
                    for (RDependency rDependency : rRuntime.getDependencies()) {
                        pw.println(rDependency.getName() + " " + rDependency.getVersion() + " " + rDependency.getOwner() + " " + rDependency.getDependencySourceType());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to write environment info file", e);
        }
    }

    private void prepareRprofile(File workDir) throws IOException {
        try (InputStream is = resourceLoader.getResource("classpath:/Rprofile").getInputStream()) {
            FileUtils.copyToFile(is, new File(workDir, ".Rprofile"));
        }
    }

    private ExecutionOutcome cleanupEnv(File directory, ExecutionOutcome outcome) {
        try {
            File cleanupScript = new File(rIsolatedRuntimeProps.getCleanupSh());
            boolean isExternal = true;

            if (!cleanupScript.exists()) {
                cleanupScript = extractToTempFile(resourceLoader, "classpath:/cleanup.sh", "ee", ".sh");
                isExternal = false;
            }
            Process p = null;
            try {
                ProcessBuilder pb = new ProcessBuilder((String[]) ArrayUtils.addAll(rIsolatedRuntimeProps.getRunCmd(), new String[]{cleanupScript.getAbsolutePath(), directory.getAbsolutePath()}));
                p = pb.start();
                p.waitFor();
            } catch (InterruptedException ignored) {
            } finally {
                if (!isExternal) {
                    FileUtils.deleteQuietly(cleanupScript);
                }
                if (Objects.nonNull(p)) {
                    closeQuietly(p.getOutputStream());
                    closeQuietly(p.getInputStream());
                    closeQuietly(p.getErrorStream());
                }
            }
            return outcome;
        } catch (IOException e) {
            log.error("Error cleaning up environment [{}]", directory.getPath(), e);
            return outcome.addError("Error cleaning up environment: " + e.getMessage());
        }
    }

    private String[] buildRuntimeCommand(File runFile, File workingDir, String fileName, String bundlePath)
            throws FileNotFoundException {

        if (!workingDir.exists()) {
            throw new FileNotFoundException("Working directory with name" + workingDir.getAbsolutePath() + "is not exists");
        }
        File file = Paths.get(workingDir.getAbsolutePath(), fileName).toFile();
        if (file.isDirectory()) {
            throw new FileNotFoundException("file '" + fileName + "' must be a regular file");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("file '"
                    + fileName + "' is not exists in directory '" + workingDir.getAbsolutePath() + "'");
        }
        String[] command;
        if (RuntimeServiceMode.ISOLATED.equals(getRuntimeServiceMode())) {
            command = ArrayUtils.addAll(rIsolatedRuntimeProps.getRunCmd(),
                    runFile.getAbsolutePath(), workingDir.getAbsolutePath(), fileName, bundlePath);
        } else {
            command = new String[]{EXECUTION_COMMAND, fileName};
        }
        return command;
    }

    private Map<String, String> buildRuntimeEnvVariables(DataSourceUnsecuredDTO dataSource, Map<String, String> krbProps) {

        Map<String, String> environment = new HashMap<>(krbProps);
        environment.put(RUNTIME_ENV_DATA_SOURCE_NAME, sanitizeFilename(dataSource.getName()));
        environment.put(RUNTIME_ENV_DBMS_USERNAME, dataSource.getUsername());
        environment.put(RUNTIME_ENV_DBMS_PASSWORD, dataSource.getPassword());
        environment.put(RUNTIME_ENV_DBMS_TYPE, dataSource.getType().getOhdsiDB());
        environment.put(RUNTIME_ENV_CONNECTION_STRING, dataSource.getConnectionString());
        environment.put(RUNTIME_ENV_DBMS_SCHEMA, dataSource.getCdmSchema());
        environment.put(RUNTIME_ENV_TARGET_SCHEMA, dataSource.getTargetSchema());
        environment.put(RUNTIME_ENV_RESULT_SCHEMA, dataSource.getResultSchema());
        environment.put(RUNTIME_ENV_COHORT_TARGET_TABLE, dataSource.getCohortTargetTable());
        environment.put(RUNTIME_ENV_DRIVER_PATH, drivers.getPath(dataSource.getType()));
        environment.put(RUNTIME_BQ_KEYFILE, getBigQueryKeyFile(dataSource));
        environment.put(RUNTIME_ENV_PATH_KEY, RUNTIME_ENV_PATH_VALUE);
        environment.put(RUNTIME_ENV_HOME_KEY, getUserHome());
        environment.put(RUNTIME_ENV_HOSTNAME_KEY, RUNTIME_ENV_HOSTNAME_VALUE);
        environment.put(RUNTIME_ENV_LANG_KEY, RUNTIME_ENV_LANG_VALUE);
        environment.put(RUNTIME_ENV_LC_ALL_KEY, RUNTIME_ENV_LC_ALL_VALUE);

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

    private String getBigQueryKeyFile(DataSourceUnsecuredDTO dataSource) {

        return dataSource.getType().equals(DBMSType.BIGQUERY) ?
                BigQueryUtils.getBigQueryKeyPath(dataSource.getConnectionString()) : null;
    }

    private RuntimeServiceMode getRuntimeServiceMode() {
        return StringUtils.isNotBlank(rIsolatedRuntimeProps.getArchive()) ? RuntimeServiceMode.ISOLATED : RuntimeServiceMode.SINGLE;
    }


    private static String sanitizeFilename(String filename) {
        return Objects.requireNonNull(filename).replaceAll("[<>:\"/\\\\|?*\\u0000]", "");
    }

    @SuppressWarnings("SameParameterValue")
    private static File extractToTempFile(ResourceLoader loader, String resourceName, String prefix, String suffix) throws IOException {
        File runFile = Files.createTempFile(prefix, suffix).toFile();
        try (InputStream in = loader.getResource(resourceName).getInputStream()) {
            FileUtils.copyToFile(in, runFile);
        }
        return runFile;
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        // TODO Once on JDK9+, replace with CompletableFuture.failedFuture(Throwable ex)
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(throwable);
        return result;
    }

}

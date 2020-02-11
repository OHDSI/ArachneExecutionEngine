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

package com.odysseusinc.arachne.executionengine.service.impl;

import static org.apache.commons.io.IOUtils.closeQuietly;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.util.BigQueryUtils;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.config.properties.HiveBulkLoadProperties;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.service.RuntimeService;
import com.odysseusinc.arachne.executionengine.util.AnalysisCallback;
import com.odysseusinc.arachne.executionengine.util.FileResourceUtils;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import com.odysseusinc.datasourcemanager.krblogin.RuntimeServiceMode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class RuntimeServiceImpl implements RuntimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeServiceImpl.class);

    private static final String EXECUTION_COMMAND = "Rscript";
    private static final String ERROR_BUILDING_COMMAND_LOG = "Error building runtime command";
    private static final String EXECUTING_LOG = "Executing:{}";
    private static final String DESTROYING_PROCESS_LOG = "timeout exceeded, destroying process";
    private static final String EXECUTION_SUCCESS_LOG = "Execution id={} success, ExitCode='{}'";
    private static final String EXECUTION_FAILURE_LOG = "Execution id={} failure, ExitCode='{}'";
    private static final String STDOUT_LOG = "stdout:\n{}";
    private static final String STDOUT_LOG_DIFF = "stdout update:\n{}";
    private static final String DELETE_DIR_ERROR_LOG = "Can't delete analysis directory: '{}'";

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

    private final ThreadPoolTaskExecutor taskExecutor;
    private final CallbackService callbackService;
    private final ResourceLoader resourceLoader;

    @Value("${runtime.timeOutSec}")
    private int runtimeTimeOutSec;
    @Value("${submission.update.interval}")
    private int submissionUpdateInterval;
    @Value("${drivers.location.impala}")
    private String impalaDriversLocation;
    @Value("${drivers.location.bq}")
    private String bqDriversLocation;
    @Value("${drivers.location.netezza}")
    private String netezzaDriversLocation;
    @Value("${drivers.location.hive}")
    private String hiveDriversLocation;
    @Value("${bulkload.enableMPP}")
    private Boolean enableMPP;
    private final HiveBulkLoadProperties hiveBulkLoadProperties;

    private RIsolatedRuntimeProperties rIsolatedRuntimeProps;


    @Autowired
    public RuntimeServiceImpl(ThreadPoolTaskExecutor taskExecutor,
                              CallbackService callbackService,
                              ResourceLoader resourceLoader,
                              HiveBulkLoadProperties hiveBulkLoadProperties,
                              RIsolatedRuntimeProperties rIsolatedRuntimeProps) {

        this.taskExecutor = taskExecutor;
        this.callbackService = callbackService;
        this.resourceLoader = resourceLoader;
        this.hiveBulkLoadProperties = hiveBulkLoadProperties;
        this.rIsolatedRuntimeProps = rIsolatedRuntimeProps;
    }

    @PostConstruct
    public void init() {

        if (RuntimeServiceMode.ISOLATED.equals(getRuntimeServiceMode())) {
            LOGGER.info("Runtime service running in ISOLATED environment mode");
        } else {
            LOGGER.info("Runtime service running in SINGLE mode");
        }
    }

    @Override
    public RuntimeServiceMode getRuntimeServiceMode() {

        return StringUtils.isNotBlank(rIsolatedRuntimeProps.getArchive()) ? RuntimeServiceMode.ISOLATED : RuntimeServiceMode.SINGLE;
    }

    @Override
    @FileDescriptorCount
    public Future analyze(AnalysisSyncRequestDTO analysis, File file, StdoutHandlerParams stdoutHandlerParams, AnalysisCallback analysisCallback, KrbConfig krbConfig) {

        return taskExecutor.submit(() -> {
            try {
                Long id = analysis.getId();
                String executableFileName = analysis.getExecutableFileName();
                DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
                RuntimeFinishStatus finishStatus;
                try {
                    File runFile = prepareEnvironment();
                    prepareRprofile(file);
                    try {
                        String[] command = buildRuntimeCommand(runFile, file, executableFileName);

                        final Map<String, String> envp = buildRuntimeEnvVariables(dataSource, krbConfig.getIsolatedRuntimeEnvs());
                        envp.put(RUNTIME_ANALYSIS_ID, analysis.getId().toString());
                        finishStatus = runtime(command, envp, file, runtimeTimeOutSec, id, stdoutHandlerParams);
                        AnalysisResultStatusDTO resultStatusDTO = finishStatus.exitCode == 0
                                ? AnalysisResultStatusDTO.EXECUTED : AnalysisResultStatusDTO.FAILED;
                        cleanupEnvironment(file);
                        analysisCallback.execute(resultStatusDTO, finishStatus.stdout, file, null);
                    } finally {
                        if (!isExternalJail()) {
                            FileUtils.deleteQuietly(runFile);
                        }
                        FileUtils.deleteQuietly(krbConfig.getComponents().getKeytabPath().toFile());
                        if (RuntimeServiceMode.ISOLATED == krbConfig.getMode()) {
                            FileUtils.deleteQuietly(krbConfig.getConfPath().toFile());
                        }
                    }
                } catch (FileNotFoundException ex) {
                    LOGGER.error(ERROR_BUILDING_COMMAND_LOG, ex);
                    throw ex;
                } catch (InterruptedException | IOException | ExecutionException | TimeoutException ex) {
                    LOGGER.error("", ex);
                    throw ex;
                }
            } catch (Throwable t) {
                LOGGER.error("Analysis with id={} failed to execute in Runtime Service", analysis.getId(), t);
                analysisCallback.execute(null, null, file, t);
            }
        });
    }

    private void prepareRprofile(File workDir) throws IOException {

        try(InputStream is = resourceLoader.getResource("classpath:/Rprofile").getInputStream();
            FileOutputStream out = new FileOutputStream(new File(workDir, ".Rprofile"))) {
            IOUtils.copy(is, out);
        }
    }

    private File prepareEnvironment() throws IOException {

        return isExternalJail()
                ? new File(rIsolatedRuntimeProps.getJailSh())
                : FileResourceUtils.extractResourceToTempFile(resourceLoader, "classpath:/jail.sh", "ee", ".sh");
    }

    private boolean isExternalJail() {

        return new File(rIsolatedRuntimeProps.getJailSh()).isFile();
    }

    private void cleanupEnvironment(File directory) throws IOException {

        File cleanupScript = new File(rIsolatedRuntimeProps.getCleanupSh());
        boolean isExternal = true;

        if (!cleanupScript.exists()) {
            cleanupScript = FileResourceUtils.extractResourceToTempFile(resourceLoader, "classpath:/cleanup.sh", "ee", ".sh");
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
    }

    private String[] buildRuntimeCommand(File runFile, File workingDir, String fileName) throws FileNotFoundException {

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
            command = (String[]) ArrayUtils.addAll(rIsolatedRuntimeProps.getRunCmd(), new String[]{runFile.getAbsolutePath(), workingDir.getAbsolutePath(), fileName, rIsolatedRuntimeProps.getArchive()});
        } else {
            command = new String[]{EXECUTION_COMMAND, fileName};
        }
        return command;
    }

    private Map<String, String> buildRuntimeEnvVariables(DataSourceUnsecuredDTO dataSource, Map<String, String> krbProps) {

        Map<String, String> environment = new HashMap<>(krbProps);
        environment.put(RUNTIME_ENV_DATA_SOURCE_NAME, dataSource.getName());
        environment.put(RUNTIME_ENV_DBMS_USERNAME, dataSource.getUsername());
        environment.put(RUNTIME_ENV_DBMS_PASSWORD, dataSource.getPassword());
        environment.put(RUNTIME_ENV_DBMS_TYPE, dataSource.getType().getOhdsiDB());
        environment.put(RUNTIME_ENV_CONNECTION_STRING, dataSource.getConnectionString());
        environment.put(RUNTIME_ENV_DBMS_SCHEMA, dataSource.getCdmSchema());
        environment.put(RUNTIME_ENV_TARGET_SCHEMA, dataSource.getTargetSchema());
        environment.put(RUNTIME_ENV_RESULT_SCHEMA, dataSource.getResultSchema());
        environment.put(RUNTIME_ENV_COHORT_TARGET_TABLE, dataSource.getCohortTargetTable());
        environment.put(RUNTIME_ENV_DRIVER_PATH, getDriversPath(dataSource));
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

    private String getDriversPath(DataSourceUnsecuredDTO dataSource) {

        switch (dataSource.getType()) {
            case IMPALA:
                return impalaDriversLocation;
            case BIGQUERY:
                return bqDriversLocation;
            case NETEZZA:
                return netezzaDriversLocation;
            case HIVE:
                return hiveDriversLocation;
            default:
                return null;
        }
    }

    private RuntimeFinishStatus runtime(String[] command,
                                        Map<String, String> envp,
                                        File activeDir,
                                        int timeout,
                                        Long submissionId,
                                        StdoutHandlerParams stdoutHandlerParams) throws IOException, InterruptedException, ExecutionException, TimeoutException {

        final ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(activeDir)
                .redirectErrorStream(true);
        processBuilder.environment().putAll(envp);
        Process process = null;
        try {
            process = processBuilder.start();
            final ExecutorService executorService = Executors.newSingleThreadExecutor();
            final StdoutHandler stdoutHandler = new StdoutHandler(process, stdoutHandlerParams);
            final Future<String> future = executorService.submit(stdoutHandler);
            StringBuilder commandBuilder = new StringBuilder();
            Arrays.stream(command).forEach(c -> commandBuilder.append(" ").append(c));
            LOGGER.info(EXECUTING_LOG, commandBuilder.toString());
            process.waitFor(timeout, TimeUnit.SECONDS);
            if (process.isAlive()) {
                process.destroy();
                LOGGER.warn(DESTROYING_PROCESS_LOG);
            }
            final String stdout = future.get(submissionUpdateInterval * 2, TimeUnit.MILLISECONDS);
            if (process.exitValue() == 0) {
                LOGGER.info(EXECUTION_SUCCESS_LOG, submissionId, process.exitValue());
            } else {
                LOGGER.warn(EXECUTION_FAILURE_LOG, submissionId, process.exitValue());
            }
            LOGGER.debug(STDOUT_LOG, stdout);
            return new RuntimeFinishStatus(process.exitValue(), stdout);
        } finally {
            if (Objects.nonNull(process)) {
                closeQuietly(process.getOutputStream());
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
            }
        }
    }

    private class RuntimeFinishStatus {
        private final int exitCode;
        private final String stdout;

        private RuntimeFinishStatus(int exitCode, String stdout) {

            this.exitCode = exitCode;
            this.stdout = stdout;
        }
    }

    private class StdoutHandler implements Callable<String> {

        private final Process process;
        private final Integer submissionUpdateInterval;
        private final Consumer<String> callback;

        private StdoutHandler(Process process, StdoutHandlerParams stdoutHandlerParams) {

            this.process = process;
            this.submissionUpdateInterval = stdoutHandlerParams.getSubmissionUpdateInterval();
            this.callback = stdoutHandlerParams.getCallback();
        }

        @Override
        public String call() throws Exception {

            StringBuilder stdout = new StringBuilder();
            try {
                do {
                    Thread.sleep(submissionUpdateInterval);
                    InputStream inputStream = process.getInputStream();
                    final String stdoutDiff = getStdoutDiff(inputStream);
                    stdout.append(stdoutDiff);
                    if (callback != null) {
                        callback.accept(stdoutDiff);
                    }
                    if (!stdoutDiff.isEmpty()) {
                        LOGGER.debug(STDOUT_LOG_DIFF, stdoutDiff);
                    }
                } while (process.isAlive());
            } catch (IOException e) {
                LOGGER.warn("Process was destroyed during attempt to write stdout");
            }
            return stdout.toString();
        }

        private String getStdoutDiff(InputStream stream) throws IOException {

            int available = stream.available();
            if (available > 0) {
                byte[] buffer = new byte[available];
                //noinspection ResultOfMethodCallIgnored
                stream.read(buffer);
                return new String(buffer);
            }
            return "";
        }
    }
}

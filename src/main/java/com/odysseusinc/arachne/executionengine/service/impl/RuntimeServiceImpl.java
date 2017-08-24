package com.odysseusinc.arachne.executionengine.service.impl;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.service.RuntimeService;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class RuntimeServiceImpl implements RuntimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeServiceImpl.class);

    private static final String EXECUTION_COMMAND = "Rscript";
    private static final String ERROR_BUILDING_COMMAND_LOG = "Error building runtime command";
    private static final String EXECUTING_LOG = "Executing:{}";
    private static final String DESTROYING_PROCESS_LOG = "timeout exceeded, destroying process, output:\n{}";
    private static final String EXECUTION_SUCCESS_LOG = "Execution success, ExitCode='{}'";
    private static final String EXECUTION_FAILURE_LOG = "Execution failure, ExitCode='{}'";
    private static final String STDOUT_LOG = "stdout:\n{}";
    private static final String STDOUT_LOG_DIFF = "stdout update:\n{}";
    private static final String DELETE_DIR_ERROR_LOG = "Can't delete analysis directory: '{}'";

    private static final String RUNTIME_ENV_DBMS_USERNAME = "DBMS_USERNAME";
    private static final String RUNTIME_ENV_DBMS_PASSWORD = "DBMS_PASSWORD";
    private static final String RUNTIME_ENV_DBMS_TYPE = "DBMS_TYPE";
    private static final String RUNTIME_ENV_CONNECTION_STRING = "CONNECTION_STRING";
    private static final String RUNTIME_ENV_DBMS_SCHEMA = "DBMS_SCHEMA";
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

    private final TaskExecutor taskExecutor;
    private final CallbackService callbackService;

    @Value("${runtime.timeOutSec}")
    private int runtimeTimeOutSec;
    @Value("${submission.update.interval}")
    private int submissionUpdateInterval;

    @Autowired
    public RuntimeServiceImpl(TaskExecutor taskExecutor, CallbackService callbackService) {

        this.taskExecutor = taskExecutor;
        this.callbackService = callbackService;
    }

    @Override
    public void analyze(AnalysisRequestDTO analysis, File file, Boolean compressedResult, Long chunkSize) {

        taskExecutor.execute(() -> {
            Long id = analysis.getId();
            String callbackPassword = analysis.getCallbackPassword();
            String executableFileName = analysis.getExecutableFileName();
            String updateStatusCallback = analysis.getUpdateStatusCallback();
            DataSourceDTO dataSource = analysis.getDataSource();
            RuntimeFinishStatus finishStatus;
            final File zipDir = com.google.common.io.Files.createTempDir();
            try {
                String[] command = buildRuntimeCommand(file, executableFileName);
                final Map<String, String> envp = buildRuntimeEnvVariables(dataSource);
                finishStatus = runtime(command, envp, file, runtimeTimeOutSec, updateStatusCallback, id, callbackPassword);
                AnalysisResultDTO result = new AnalysisResultDTO();
                result.setId(id);
                result.setRequested(analysis.getRequested());
                if (finishStatus != null) {
                    result.setStdout(finishStatus.stdout);
                    result.setStatus(finishStatus.exitCode == 0
                            ? AnalysisResultStatusDTO.EXECUTED : AnalysisResultStatusDTO.FAILED);
                }

                List<FileSystemResource> resultFSResources
                        = AnalisysUtils.getFileSystemResources(analysis, file, compressedResult, chunkSize, zipDir);
                callbackService.sendAnalysisResult(analysis.getResultCallback(), callbackPassword, result, resultFSResources);
            } catch (FileNotFoundException ex) {
                LOGGER.error(ERROR_BUILDING_COMMAND_LOG, ex);
            } catch (InterruptedException | IOException ex) {
                LOGGER.error("", ex);
            } finally {
                try {
                    FileUtils.deleteDirectory(file);
                    FileUtils.deleteQuietly(zipDir);
                } catch (IOException ex) {
                    LOGGER.warn(DELETE_DIR_ERROR_LOG, file.getAbsolutePath());
                }
            }
        });
    }

    private String[] buildRuntimeCommand(File workingDir, String fileName) throws FileNotFoundException {

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
        return new String[]{EXECUTION_COMMAND, fileName};
    }

    private Map<String, String> buildRuntimeEnvVariables(DataSourceDTO dataSource) {

        Map<String, String> environment = new HashMap<>();
        environment.put(RUNTIME_ENV_DBMS_USERNAME, dataSource.getUsername());
        environment.put(RUNTIME_ENV_DBMS_PASSWORD, dataSource.getPassword());
        environment.put(RUNTIME_ENV_DBMS_TYPE, dataSource.getType().toString().toLowerCase());
        environment.put(RUNTIME_ENV_CONNECTION_STRING, dataSource.getConnectionString());
        environment.put(RUNTIME_ENV_DBMS_SCHEMA, dataSource.getCdmSchema());
        environment.put(RUNTIME_ENV_PATH_KEY, RUNTIME_ENV_PATH_VALUE);
        environment.put(RUNTIME_ENV_HOME_KEY, RUNTIME_ENV_HOME_VALUE);
        environment.put(RUNTIME_ENV_HOSTNAME_KEY, RUNTIME_ENV_HOSTNAME_VALUE);
        environment.put(RUNTIME_ENV_LANG_KEY, RUNTIME_ENV_LANG_VALUE);
        environment.put(RUNTIME_ENV_LC_ALL_KEY, RUNTIME_ENV_LC_ALL_VALUE);

        return environment;
    }

    private RuntimeFinishStatus runtime(String[] command,
                                        Map<String, String> envp,
                                        File activeDir,
                                        int timeout,
                                        String updateUrl,
                                        Long submissionId,
                                        String password) throws IOException, InterruptedException {

        final ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(activeDir)
                .redirectErrorStream(true);
        processBuilder.environment().putAll(envp);
        final Process process = processBuilder.start();
        StringBuilder commandBuilder = new StringBuilder();
        Arrays.stream(command).forEach(c -> commandBuilder.append(" ").append(c));
        LOGGER.info(EXECUTING_LOG, commandBuilder.toString());
        long startedAt = System.currentTimeMillis();
        long thresholdAt = startedAt + (timeout * 1000);
        StringBuffer stdout = new StringBuffer();
        do {
            Thread.sleep(submissionUpdateInterval);
            InputStream inputStream = process.getInputStream();
            final String stdoutDiff = getStdoutDiff(inputStream);
            stdout.append(stdoutDiff);
            callbackService.updateAnalysisStatus(updateUrl, submissionId, stdoutDiff, password);
            if (!stdoutDiff.isEmpty()) {
                LOGGER.debug(STDOUT_LOG_DIFF, stdoutDiff);
            }
        } while (process.isAlive() && System.currentTimeMillis() < thresholdAt);
        if (process.isAlive()) {
            process.destroy();
            LOGGER.warn(DESTROYING_PROCESS_LOG, stdout);
        }
        if (process.exitValue() == 0) {
            LOGGER.info(EXECUTION_SUCCESS_LOG, process.exitValue());
        } else {
            LOGGER.warn(EXECUTION_FAILURE_LOG, process.exitValue());
        }
        LOGGER.debug(STDOUT_LOG, stdout.toString());
        return new RuntimeFinishStatus(process.exitValue(), stdout.toString());
    }

    private static String getStdoutDiff(InputStream stream) throws IOException {

        int available = stream.available();
        if (available > 0) {
            byte[] buffer = new byte[available];
            //noinspection ResultOfMethodCallIgnored
            stream.read(buffer);
            return new String(buffer);
        }
        return "";
    }

    private class RuntimeFinishStatus {
        private final int exitCode;
        private final String stdout;

        private RuntimeFinishStatus(int exitCode, String stdout) {

            this.exitCode = exitCode;
            this.stdout = stdout;
        }
    }
}

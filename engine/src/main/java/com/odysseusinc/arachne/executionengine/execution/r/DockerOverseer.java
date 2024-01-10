package com.odysseusinc.arachne.executionengine.execution.r;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.R;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import com.odysseusinc.arachne.executionengine.execution.AbstractOverseer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Slf4j
@Getter
public class DockerOverseer extends AbstractOverseer {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1) {{
        setRemoveOnCancelPolicy(true);
    }};
    private final CompletableFuture<String> init;

    /**
     * Last position in stdout that was submitted to callback.
     */
    private volatile int pos;
    private final DockerClient client;

    public DockerOverseer(
            long id, DockerClient client, Instant started, int timeoutSec, StringBuffer stdout, CompletableFuture<String> init,
            int updateInterval, BiConsumer<String, String> callback, String image, int killTimeoutSec
    ) {
        super(id, callback, started, image, killTimeoutSec, stdout, init.handle((containerId, throwable) -> {
            if (throwable != null) {
                String out = stdout.append("\r\n").append(ExceptionUtils.getStackTrace(throwable)).toString();
                return new ExecutionOutcome(Stage.INITIALIZE, throwable.getMessage(), out);
            } else {
                LogContainerCmd cmd = client.logContainerCmd(containerId).withStdOut(true).withStdErr(true).withFollowStream(true);
                cmd.exec(logAdapter(id, stdout));
                Integer exitCode = client.waitContainerCmd(containerId).exec(new WaitContainerResultCallback()).awaitStatusCode(timeoutSec, TimeUnit.SECONDS);
                log.info("Execution [{}] Rscript exit code {}", id, exitCode);
                String out = stdout.toString();
                return (exitCode == 0)
                        ? new ExecutionOutcome(Stage.COMPLETED, null, out)
                        : new ExecutionOutcome(Stage.EXECUTE, "Exit code " + exitCode, out);
            }
        }));
        pos = stdout.length();
        this.client = client;
        init.thenAccept(containerId ->
                executor.scheduleWithFixedDelay(this::writeLogs, updateInterval, updateInterval, TimeUnit.MILLISECONDS)
        );
        this.init = init;
    }

    @Override
    public CompletableFuture<ExecutionOutcome> abort() {
        if (!init.isDone()) {
            init.cancel(true);
        }

        return init.handle((containerId, throwable) -> {
            if (containerId == null) {
                if (throwable != null) {
                    log.error("Error during initialization: {}", throwable.getMessage(), throwable);
                    stdout.append("\r\n\"Initialization failed: ").append(throwable.getMessage());
                    return new ExecutionOutcome(Stage.INITIALIZE, "Initialization failed: " + throwable.getMessage(), stdout.toString());
                } else {
                    stdout.append("\r\nContainer initialization was canceled");
                    return new ExecutionOutcome(Stage.ABORTED, null, stdout.toString());
                }
            } else {
                if (result.isDone()) {
                    return result.join();
                } else {
                    try {
                        client.stopContainerCmd(containerId).exec();
                        stdout.append("\r\nDocker container aborted successfully");
                        return new ExecutionOutcome(Stage.ABORTED, null, stdout.toString());
                    } catch (NotFoundException e) {
                        log.info("Container not found or already stopped: " + e.getMessage());
                        return result.join();
                    } catch (DockerException e) {
                        log.error("Error stopping the Docker container: {}", e.getMessage());
                        stdout.append("\r\n\"Error aborting Docker container:").append(e.getMessage());
                        return new ExecutionOutcome(Stage.ABORT, "Error aborting Docker container: " + e.getMessage(), stdout.toString());
                    }
                }
            }
        });

    }

    private static ResultCallback.Adapter<Frame> logAdapter(long id, StringBuffer stdout) {
        return new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame item) {
                super.onNext(item);
                String output = new String(item.getPayload(), StandardCharsets.UTF_8);
                log.debug("Execution [{}] STDOUT: {}", id, output.trim());
                stdout.append(output);
            }

            @Override
            public void onError(Throwable throwable) {
                if (!(throwable instanceof NotFoundException)) {
                    log.error("Execution [{}] error: {}", id, throwable);
                    stdout.append("Execution error: ").append(throwable.getMessage());
                    super.onError(throwable);
                }
            }
        };
    }

    @Override
    public AnalysisRequestTypeDTO getType() {
        return R;
    }

    private void writeLogs() {
        int length = stdout.length();
        if (length > pos) {
            String delta = stdout.substring(pos, length);
            callback.accept(Stage.EXECUTE, delta);
            pos = length;
        }
    }

}

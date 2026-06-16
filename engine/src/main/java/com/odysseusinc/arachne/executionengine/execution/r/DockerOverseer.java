package com.odysseusinc.arachne.executionengine.execution.r;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.R;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
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
    /**
     * Two threads: one blocks on awaitStatusCode for the whole run, the other runs the periodic log flush.
     */
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(2) {{
        setRemoveOnCancelPolicy(true);
    }};
    private final CompletableFuture<String> init;

    /**
     * Last position in stdout that was submitted to callback.
     */
    private volatile int pos;
    private final DockerClient client;
    private volatile boolean aborting = false;

    public DockerOverseer(
            long id, DockerClient client, Instant started, int timeoutSec, StringBuffer stdout, CompletableFuture<String> init,
            int updateInterval, BiConsumer<String, String> callback, String image, int killTimeoutSec
    ) {
        super(id, callback, started, image, killTimeoutSec, stdout, new CompletableFuture<>());
        pos = stdout.length();
        this.client = client;
        this.init = init;

        // Shut the executor down exactly once, when the run ends (no leak).
        outcome.whenComplete((r, t) -> executor.shutdown());

        // Only REGISTER the container-startup handler here; it never blocks, so the shared pool thread is freed instantly.
        init.whenComplete((containerId, throwable) -> {
            if (throwable != null) {
                onInitFailed(throwable);
            } else {
                onContainerStarted(containerId, timeoutSec, updateInterval);
            }
        });
    }

    private void onInitFailed(Throwable throwable) {
        if (aborting) {
            outcome.complete(new ExecutionOutcome(Stage.ABORTED, null, stdout.toString()));
        } else {
            String out = stdout.append("\r\n").append(ExceptionUtils.getStackTrace(throwable)).toString();
            outcome.complete(new ExecutionOutcome(Stage.INITIALIZE, throwable.getMessage(), out));
        }
    }

    private void onContainerStarted(String containerId, int timeoutSec, int updateInterval) {
        // Flush logs to the callback periodically (one pool thread).
        executor.scheduleWithFixedDelay(this::writeLogs, updateInterval, updateInterval, TimeUnit.MILLISECONDS);
        // Block on the wait on our OWN pool (another thread), not on the shared analysisTaskExecutor.
        executor.execute(() -> awaitAndComplete(containerId, timeoutSec));
    }

    private void awaitAndComplete(String containerId, int timeoutSec) {
        try {
            client.logContainerCmd(containerId).withStdOut(true).withStdErr(true).withFollowStream(true).exec(logAdapter(id, stdout));
            Integer exitCode = client.waitContainerCmd(containerId).exec(new WaitContainerResultCallback()).awaitStatusCode(timeoutSec, TimeUnit.SECONDS);
            completeFromExit(exitCode);
        } catch (Exception e) {
            log.error("Execution [{}] error waiting for container", id, e);
            writeLogs();
            outcome.complete(aborting
                    ? new ExecutionOutcome(Stage.ABORTED, null, stdout.toString())
                    : new ExecutionOutcome(Stage.EXECUTE, e.getMessage(), stdout.toString()));
        }
    }

    private void completeFromExit(int exitCode) {
        writeLogs();
        log.info("Execution [{}] Rscript exit code {}", id, exitCode);
        String out = stdout.toString();
        outcome.complete(exitCode == 0
                ? new ExecutionOutcome(Stage.COMPLETED, null, out)
                : aborting
                        ? new ExecutionOutcome(Stage.ABORTED, null, out)
                        : new ExecutionOutcome(Stage.EXECUTE, "Exit code " + exitCode, out));
    }

    @Override
    public CompletableFuture<ExecutionOutcome> abort() {
        aborting = true;
        init.thenAccept(this::stopContainer);
        return CompletableFuture.completedFuture(new ExecutionOutcome(Stage.ABORT, null, stdout.toString()));
    }

    private void stopContainer(String containerId) {
        try {
            client.stopContainerCmd(containerId).withTimeout(0).exec();
            log.info("Execution [{}] stop command sent to Docker container", id);
        } catch (NotFoundException e) {
            log.info("Execution [{}] container not found or already stopped: {}", id, e.getMessage());
        } catch (DockerException e) {
            log.error("Execution [{}] error stopping Docker container: {}", id, e.getMessage());
        }
    }

    private static ResultCallback.Adapter<Frame> logAdapter(long id, StringBuffer stdout) {
        return new ResultCallback.Adapter<>() {
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
                    log.error("Execution [{}] error: {}", id, throwable.getMessage(), throwable);
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

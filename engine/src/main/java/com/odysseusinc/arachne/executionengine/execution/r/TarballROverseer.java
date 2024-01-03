package com.odysseusinc.arachne.executionengine.execution.r;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import com.odysseusinc.arachne.executionengine.execution.AbstractOverseer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TarballROverseer extends AbstractOverseer {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1) {{
        setRemoveOnCancelPolicy(true);
    }};

    private final Process process;
    private final BufferedReader reader;
    private final ScheduledFuture<?> logFlush;

    /**
     * Creates a new process overseer.
     *  @param id             Execution identifier, for logging.
     * @param process        Process to manage
     * @param timeout        Timeout (in seconds). Once this amount of time is passed, the process will be terminated.
     * @param callback       Consumer to send progress. First argument is current stage, second is log.
     * @param updateInterval Log polling interval, in milliseconds.
     * @param started        The moment when execution has been requested
     * @param environment    Id of the descriptor used for execution
     * @param killTimeout    Timeout (in seconds to wait for the process to die after trying to kill it).
     */
    public TarballROverseer(
            long id, Process process, int timeout, BiConsumer<String, String> callback, int updateInterval, Instant started, String environment, int killTimeout
    ) {
        super(id, callback, started, environment, killTimeout, new StringBuffer(), new CompletableFuture<ExecutionOutcome>());
        this.process = process;

        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        executor.schedule(() -> {
            if (process.isAlive()) {
                log.info("Terminating [{}] after {} seconds of inactivity", id, timeout);
                terminate();
            }
        }, timeout, TimeUnit.SECONDS);
        log.info("For [{}], initialized watchdog job with {} seconds timeout", id, timeout);

        logFlush = executor.scheduleAtFixedRate(() -> writeLogs(Stage.EXECUTE), updateInterval, updateInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public AnalysisRequestTypeDTO getType() {
        return AnalysisRequestTypeDTO.R;
    }

    @Override
    public CompletableFuture<ExecutionOutcome> abort() {
        if (process.isAlive()) {
            log.info("Overseer [{}] processing abort request", id);
            if (terminate()) {
                outcome.complete(new ExecutionOutcome(Stage.ABORTED, null, stdout.toString()));
            } else {
                callback.accept(Stage.ABORT, "Timed out waiting for termination");
            }
        } else {
            log.info("Overseer [{}] received abort, but process exited already", id);
        }
        return result;
    }

    private void writeLogs(String stage) {
        try {
            char[] buffer = new char[1024];
            StringBuilder sb = new StringBuilder();
            while (reader.ready()) {
                int read = reader.read(buffer);
                sb.append(buffer, 0, read);
            }
            if (sb.length() != 0) {
                String delta = sb.toString();
                log.info("STDOUT [{}]:\n{}", id, delta);
                stdout.append(delta);
                callback.accept(stage, delta);
            }
            // Do this after flushing the log to ensure that end of log doesn't get lost
            if (!process.isAlive()) {
                log.info("Overseer [{}] shutdown", id);
                complete(process.exitValue());
            }
        } catch (IOException e) {
            if (process.isAlive()) {
                log.error("Overseer [{}] unable to read log on a live process: {}", id, e.getMessage());
                callback.accept(stage, "=== Error reading log ===");
                logFlush.cancel(false);
            } else {
                log.warn("Overseer [{}] shutdown, dead process, read log error: {}", id, e.getMessage());
                complete(process.exitValue());
            }
        }
    }

    private void complete(int exitValue) {
        executor.shutdown();
        ExecutionOutcome outcome = (exitValue == 0)
                ? new ExecutionOutcome(Stage.COMPLETED, null, stdout.toString())
                : new ExecutionOutcome(Stage.EXECUTE, "Exit code " + exitValue, stdout.toString());
        this.outcome.complete(outcome);
    }

    private boolean terminate() {
        // TODO Can race with cancel?
        writeLogs(Stage.ABORT);
        boolean dead = waitForKill();
        if (dead) {
            log.info("Terminated [{}]", id);
            executor.shutdownNow();
            log.info("Shut down overseer for [{}]", id);
        } else {
            // We don't want to shut down executor in order to get some logs
            log.error("Overseer [{}] failed to terminate analysis process", id);
        }
        return dead;
    }

    private boolean waitForKill() {
        try {
            // Will not kill whole process tree on windows. The fundamental problem here is that (unlike Unix)
            // Windows doesn't maintain parent-child relationships between processes. A process can kill
            // its own immediate children, but not 'grand-children' because it has no way of finding them.
            return process.destroyForcibly().waitFor(killTimeoutSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("Overseer [{}] interrupted waiting for process termination", id);
            return false;
        }
    }


}

package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.Getter;

/**
 * Dummy implementation to provide consistent status and callback reporting for analysis that failed to initialize.
 */
@Getter
public class FailedOverseer implements Overseer {
    private final Instant started;
    private final String message;
    private final AnalysisRequestTypeDTO type;
    private final CompletableFuture<ExecutionOutcome> result;
    private final ExecutionOutcome outcome;
    private final Throwable error;

    public FailedOverseer(Instant started, String message, AnalysisRequestTypeDTO type, Throwable error) {
        this.started = started;
        this.message = message;
        this.type = type;
        this.error = error;
        outcome = new ExecutionOutcome(Stage.INITIALIZE, message, message);
        result = CompletableFuture.completedFuture(outcome);
    }

    @Override
    public String getStdout() {
        return getMessage();
    }

    @Override
    public CompletableFuture<ExecutionOutcome> abort() {
        return result;
    }

    @Override
    public String getEnvironment() {
        return null;
    }

    @Override
    public Overseer whenComplete(BiConsumer<ExecutionOutcome, Throwable> finalizer) {
        CompletableFuture.runAsync(() -> finalizer.accept(outcome, error));
        return this;
    }
}

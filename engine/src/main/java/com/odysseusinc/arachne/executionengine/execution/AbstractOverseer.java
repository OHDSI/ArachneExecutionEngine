package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.Getter;

public abstract class AbstractOverseer implements Overseer {
    protected final long id;
    protected final StringBuffer stdout;
    protected final BiConsumer<String, String> callback;
    /**
     * A pure execution result
     */
    protected final CompletableFuture<ExecutionOutcome> outcome;
    @Getter
    protected final Instant started;
    @Getter
    protected final String environment;
    protected final int killTimeout;
    /**
     * Execution result after applying post-processing
     */
    @Getter
    protected volatile CompletableFuture<ExecutionOutcome> result;

    public AbstractOverseer(long id, BiConsumer<String, String> callback, Instant started, String environment, int killTimeout, StringBuffer stdout, CompletableFuture<ExecutionOutcome> outcome) {
        this.id = id;
        this.callback = callback;
        this.started = started;
        this.environment = environment;
        this.killTimeout = killTimeout;
        result = this.outcome = outcome;
        this.stdout = stdout;
    }

    @Override
    public String getStdout() {
        return stdout.toString();
    }

    @Override
    public Overseer whenComplete(BiConsumer<ExecutionOutcome, Throwable> finalizer) {
        // TODO atomic
        result = result.whenComplete(finalizer);
        return this;
    }

}

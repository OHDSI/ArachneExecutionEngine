package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface Overseer {
    Instant getStarted();

    /**
     * Provides a full current state of log.
     * For incomplete execution, this will be a snapshot at the given moment.
     */
    String getStdout();

    /**
     * Attempts to abort the execution. Returned future might not be complete immediately,
     * as it also includes finalizers amended via {@link #whenComplete}
     */
    CompletableFuture<ExecutionOutcome> abort();

    /**
     * Analysis execution type.
     */
    AnalysisRequestTypeDTO getType();

    /**
     * Execution result. This includes all the finalizers amended via {@link #whenComplete}
     */
    CompletableFuture<ExecutionOutcome> getResult();

    /**
     * A string identifying the actual execution environment.
     * For tarball - descriptor id, for docker - image name, etc
     */
    String getEnvironment();

    /**
     * Add a finalizer task to complete when execution is completed.
     * The contract is similar to {@link CompletableFuture#whenComplete(java.util.function.BiConsumer)}
     */
    Overseer whenComplete(BiConsumer<ExecutionOutcome, Throwable> finalizer);
}

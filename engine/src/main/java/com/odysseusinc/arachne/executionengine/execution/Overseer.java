package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface Overseer {
    Instant getStarted();

    String getStdout();

    CompletableFuture<ExecutionOutcome> abort();
}

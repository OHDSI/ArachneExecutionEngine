package com.odysseusinc.arachne.executionengine.model.descriptor;

import java.io.File;
import java.util.Optional;

public interface ParseStrategy {
    Optional<ExecutionRuntime> getExecutionRuntime(File file);
}

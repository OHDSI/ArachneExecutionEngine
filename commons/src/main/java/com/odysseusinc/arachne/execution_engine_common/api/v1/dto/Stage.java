package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

/**
 * An interface that holds value dictionary for execution stage.
 * Consumers MUST NOT assume this is a closed list, new falues might be added in the future.
 * For this exact reason, enum is not used.
 */
public interface Stage {
    /**
     * Initializing execution environment (intermediate).
     */
    String INITIALIZE = "INITIALIZE";

    /**
     * Main execution is in progress, e.g. R process or SQL statements are being run (intermediate).
     */
    String EXECUTE = "EXECUTE";

    /**
     * The execution has completed successfully (final).
     */
    String COMPLETED = "COMPLETED";

    /**
     * The request to abort the execution has been received (intermediate).
     */
    String ABORT = "ABORT";

    /**
     * The request to abort the execution has been completed (final).
     */
    String ABORTED = "ABORTED";

}

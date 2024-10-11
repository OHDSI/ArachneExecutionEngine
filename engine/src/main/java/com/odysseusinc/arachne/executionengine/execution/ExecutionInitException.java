package com.odysseusinc.arachne.executionengine.execution;

public class ExecutionInitException extends Exception {
    public ExecutionInitException() {
    }

    public ExecutionInitException(String message) {
        super(message);
    }

    public ExecutionInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionInitException(Throwable cause) {
        super(cause);
    }

    public ExecutionInitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

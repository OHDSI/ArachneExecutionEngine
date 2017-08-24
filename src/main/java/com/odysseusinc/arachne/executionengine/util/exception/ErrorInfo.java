package com.odysseusinc.arachne.executionengine.util.exception;

public class ErrorInfo {
    @SuppressWarnings("WeakerAccess")
    public final String url;
    @SuppressWarnings("WeakerAccess")
    public final String cause;
    @SuppressWarnings("WeakerAccess")
    public final String detail;

    public ErrorInfo(CharSequence url, Throwable ex) {

        this.url = url.toString();
        this.cause = ex.getClass().getSimpleName();
        this.detail = ex.getLocalizedMessage();
    }
}

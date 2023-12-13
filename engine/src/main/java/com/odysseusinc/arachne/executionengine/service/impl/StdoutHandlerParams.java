package com.odysseusinc.arachne.executionengine.service.impl;

import java.util.function.Consumer;

public class StdoutHandlerParams {

    private Integer submissionUpdateInterval;
    private Consumer<String> callback;

    public StdoutHandlerParams(Integer submissionUpdateInterval, Consumer<String> callback) {

        this.submissionUpdateInterval = submissionUpdateInterval;
        this.callback = callback;
    }

    public Integer getSubmissionUpdateInterval() {

        return submissionUpdateInterval;
    }

    public Consumer<String> getCallback() {

        return callback;
    }
}

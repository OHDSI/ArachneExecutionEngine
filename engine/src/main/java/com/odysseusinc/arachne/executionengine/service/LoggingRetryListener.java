package com.odysseusinc.arachne.executionengine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

public class LoggingRetryListener extends RetryListenerSupport {
    private static final Logger log = LoggerFactory.getLogger(LoggingRetryListener.class);

    @Override
    public <T, E extends Throwable> void onError(RetryContext retryContext, RetryCallback<T, E> retryCallback, Throwable throwable) {
        log.info("The result sending is failed: [{}], retry attempt: [{}]", throwable.getMessage(), retryContext.getRetryCount());
    }
}

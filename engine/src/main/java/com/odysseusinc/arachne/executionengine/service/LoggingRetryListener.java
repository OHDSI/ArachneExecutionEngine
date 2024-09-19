package com.odysseusinc.arachne.executionengine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

@Slf4j
public class LoggingRetryListener extends RetryListenerSupport {
    @Override
    public <T, E extends Throwable> void onError(RetryContext retryContext, RetryCallback<T, E> retryCallback, Throwable throwable) {
        log.info("The result sending is failed: [{}], retry attempt: [{}]", throwable.getMessage(), retryContext.getRetryCount());
    }
}

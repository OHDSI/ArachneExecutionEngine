package com.odysseusinc.arachne.executionengine.config;

import com.odysseusinc.arachne.executionengine.config.properties.CallbackRetryProperties;
import com.odysseusinc.arachne.executionengine.service.LoggingRetryListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.web.client.RestClientException;

@Configuration
@EnableConfigurationProperties(CallbackRetryProperties.class)
public class CallbackRetryConfig {

    @Bean
    public RetryTemplate successCallbackRetryTemplate(CallbackRetryProperties properties) {
        return buildRetryTemplate(properties.getSuccess());
    }

    @Bean
    public RetryTemplate failureCallbackRetryTemplate(CallbackRetryProperties properties) {
        return buildRetryTemplate(properties.getFailure());
    }

    private RetryTemplate buildRetryTemplate(CallbackRetryProperties.RetryPolicyProperties properties) {
        CallbackRetryProperties.ExponentialBackoffPolicyProperties backoffPolicyProperties = properties.getBackoffPolicy();
        RetryTemplateBuilder builder = RetryTemplate.builder()
                .maxAttempts(properties.getMaxAttempts())
                .exponentialBackoff(backoffPolicyProperties.getInitialIntervalMs(),
                        backoffPolicyProperties.getMultiplier(),
                        backoffPolicyProperties.getMaxIntervalMs())
                .withListener(new LoggingRetryListener())
                .retryOn(RestClientException.class)
                .traversingCauses();
        return builder.build();
    }
}

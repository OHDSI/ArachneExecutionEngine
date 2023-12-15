package com.odysseusinc.arachne.executionengine.config.properties;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "callback.retry")
public class CallbackRetryProperties {

    private RetryPolicyProperties success = new RetryPolicyProperties();
    private RetryPolicyProperties failure = new RetryPolicyProperties();

    public RetryPolicyProperties getSuccess() {
        return success;
    }

    public void setSuccess(RetryPolicyProperties success) {
        this.success = success;
    }

    public RetryPolicyProperties getFailure() {
        return failure;
    }

    public void setFailure(RetryPolicyProperties failure) {
        this.failure = failure;
    }

    public static class RetryPolicyProperties {
        @Min(1)
        @Max(Integer.MAX_VALUE)
        private int maxAttempts = 10;
        private ExponentialBackoffPolicyProperties backoffPolicy = new ExponentialBackoffPolicyProperties();

        public ExponentialBackoffPolicyProperties getBackoffPolicy() {
            return backoffPolicy;
        }

        public void setBackoffPolicy(ExponentialBackoffPolicyProperties backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class ExponentialBackoffPolicyProperties {
        private long initialIntervalMs = 5_000L; //5s
        private double multiplier = Math.E;
        private long maxIntervalMs = 900_000L; //15min

        public long getInitialIntervalMs() {
            return initialIntervalMs;
        }

        public void setInitialIntervalMs(long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxIntervalMs() {
            return maxIntervalMs;
        }

        public void setMaxIntervalMs(long maxIntervalMs) {
            this.maxIntervalMs = maxIntervalMs;
        }
    }
}

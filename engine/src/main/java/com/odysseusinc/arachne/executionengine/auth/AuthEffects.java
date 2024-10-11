package com.odysseusinc.arachne.executionengine.auth;

import java.util.Map;

/**
 * An abstraction that describes possible effects of using a specific authentication process on the analysis execution process.
 * Different execution services are expected to check for subinterfaces relevant to their specifics and interact with them.
 */
public interface AuthEffects {
    /**
     * Produces a map of environment variables to be added to execution.
     * Consuming this effect is optional.
     */
    interface AddEnvironmentVariables extends AuthEffects {
        Map<String, String> getEnvVars();
    }

    /**
     * Modify JDBC url before execution to add required authentication parameters.
     * Consuming this effect is mandatory, as the connection will not be authorized if auth detail is missing.
     */
    interface ModifyUrl extends AuthEffects {
        String getNewUrl();
    }

    /**
     * Cleans up any files related to authentication to prevent them from falling into the result archive.
     * Consuming this effect is mandatory to prevent sensitive information leak.
     */
    interface Cleanup extends AuthEffects {
        void cleanup();
    }
}

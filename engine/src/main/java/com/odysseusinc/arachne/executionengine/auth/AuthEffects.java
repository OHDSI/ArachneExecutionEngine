package com.odysseusinc.arachne.executionengine.auth;

import java.util.Map;

public interface AuthEffects {
    interface AddEnvironmentVariables extends AuthEffects {
        Map<String, String> getEnvVars();
    }

    interface ModifyUrl extends AuthEffects {
        String getNewUrl();
    }

    interface Cleanup extends AuthEffects {
        void cleanup();
    }
}

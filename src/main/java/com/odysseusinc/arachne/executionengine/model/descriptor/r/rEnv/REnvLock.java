package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class REnvLock {
    @JsonProperty("R")
    private REnv rEnv;
    @JsonProperty("Packages")
    private Map<String, RPackage> packageMap;

    public REnv getrEnv() {
        return rEnv;
    }

    public Map<String, RPackage> getPackageMap() {
        return packageMap;
    }
}

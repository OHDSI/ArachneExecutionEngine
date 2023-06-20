package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class REnv {
    @JsonProperty("Version")
    private String version;
    @JsonProperty("Repositories")
    private List<RRepository> repositories;

    public String getVersion() {
        return version;
    }

    public List<RRepository> getRepositories() {
        return repositories;
    }
}

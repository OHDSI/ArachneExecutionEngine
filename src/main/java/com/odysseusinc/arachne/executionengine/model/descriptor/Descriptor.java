package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class Descriptor {
    @JsonProperty
    private String id;
    @JsonProperty
    private String bundleName;
    @JsonProperty
    private String label;
    @JsonProperty
    private Set<String> osLibraries = new HashSet<>();
    @JsonProperty
    private Set<ExecutionRuntime> executionRuntimes = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Set<String> getOsLibraries() {
        return osLibraries;
    }

    public void setOsLibraries(Set<String> osLibraries) {
        this.osLibraries = osLibraries;
    }

    public Set<ExecutionRuntime> getExecutionRuntimes() {
        return executionRuntimes;
    }

    public void setExecutionRuntimes(Set<ExecutionRuntime> executionRuntimes) {
        this.executionRuntimes = executionRuntimes;
    }
}

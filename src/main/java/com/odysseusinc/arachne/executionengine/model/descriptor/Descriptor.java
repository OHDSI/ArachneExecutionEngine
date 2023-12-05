package com.odysseusinc.arachne.executionengine.model.descriptor;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Descriptor {
    @JsonProperty
    private String id;
    @JsonProperty
    private String bundleName;
    @JsonProperty
    private String label;
    @JsonProperty
    private List<String> osLibraries = new ArrayList<>();
    @JsonProperty
    private ExecutionRuntime[] executionRuntimes = new ExecutionRuntime[0];

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

    public List<String> getOsLibraries() {
        return osLibraries;
    }

    public void setOsLibraries(List<String> osLibraries) {
        this.osLibraries = osLibraries;
    }

    public ExecutionRuntime[] getExecutionRuntimes() {
        return executionRuntimes;
    }

    public void setExecutionRuntimes(ExecutionRuntime[] executionRuntimes) {
        this.executionRuntimes = executionRuntimes;
    }
}

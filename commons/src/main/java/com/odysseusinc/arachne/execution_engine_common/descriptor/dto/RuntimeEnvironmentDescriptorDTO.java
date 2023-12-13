package com.odysseusinc.arachne.execution_engine_common.descriptor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RuntimeEnvironmentDescriptorDTO {
    @JsonProperty
    private String id;
    @JsonProperty
    private String bundleName;
    @JsonProperty
    private String label;
    @JsonProperty
    private List<ExecutionRuntimeDTO> executionRuntimes;

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

    public List<ExecutionRuntimeDTO> getExecutionRuntimes() {
        return executionRuntimes;
    }

    public void setExecutionRuntimes(List<ExecutionRuntimeDTO> executionRuntimes) {
        this.executionRuntimes = executionRuntimes;
    }
}

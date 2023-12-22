package com.odysseusinc.arachne.execution_engine_common.descriptor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RuntimeEnvironmentDescriptorsDTO {

    @JsonProperty
    private boolean docker;
    @JsonProperty
    private List<RuntimeEnvironmentDescriptorDTO> descriptors;

    public RuntimeEnvironmentDescriptorsDTO() {
    }

    public RuntimeEnvironmentDescriptorsDTO(List<RuntimeEnvironmentDescriptorDTO> descriptors) {
        this.descriptors = descriptors;
    }

    public RuntimeEnvironmentDescriptorsDTO(boolean docker, List<RuntimeEnvironmentDescriptorDTO> descriptors) {
        this.docker = docker;
        this.descriptors = descriptors;
    }

    public List<RuntimeEnvironmentDescriptorDTO> getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(List<RuntimeEnvironmentDescriptorDTO> descriptors) {
        this.descriptors = descriptors;
    }

    public boolean isDocker() {
        return docker;
    }

    public void setDocker(boolean docker) {
        this.docker = docker;
    }
}

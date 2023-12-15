package com.odysseusinc.arachne.execution_engine_common.descriptor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RuntimeEnvironmentDescriptorsDTO {
    @JsonProperty
    private List<RuntimeEnvironmentDescriptorDTO> descriptors;

    public RuntimeEnvironmentDescriptorsDTO() {
    }

    public RuntimeEnvironmentDescriptorsDTO(List<RuntimeEnvironmentDescriptorDTO> descriptors) {
        this.descriptors = descriptors;
    }

    public List<RuntimeEnvironmentDescriptorDTO> getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(List<RuntimeEnvironmentDescriptorDTO> descriptors) {
        this.descriptors = descriptors;
    }
}

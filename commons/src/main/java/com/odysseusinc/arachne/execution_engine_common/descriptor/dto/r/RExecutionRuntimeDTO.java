package com.odysseusinc.arachne.execution_engine_common.descriptor.dto.r;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.ExecutionRuntimeDTO;

import java.util.ArrayList;
import java.util.List;

public class RExecutionRuntimeDTO implements ExecutionRuntimeDTO {
    @JsonProperty
    private String version;
    @JsonProperty
    private List<RDependencyDTO> dependencies = new ArrayList<>();

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.R;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<RDependencyDTO> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<RDependencyDTO> dependencies) {
        this.dependencies = dependencies;
    }
}

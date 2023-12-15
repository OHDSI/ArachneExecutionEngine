package com.odysseusinc.arachne.execution_engine_common.descriptor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.r.RExecutionRuntimeDTO;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "runtimeType",
        visible = false)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RExecutionRuntimeDTO.class, name = "R")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ExecutionRuntimeDTO {
    @JsonProperty
    RuntimeType getRuntimeType();

    String getVersion();
}

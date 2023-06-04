package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "runtimeType",
        visible = false)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RExecutionRuntime.class, name = "R")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ExecutionRuntime {
    RuntimeType getRuntimeType();

    String getVersion();

    List<String> createInstallScripts();

    boolean matches(ExecutionRuntime otherRuntime);
}

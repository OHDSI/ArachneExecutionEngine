package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = false)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RExecutionRuntime.class, name = "R")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ExecutionRuntime {
    RuntimeType getType();

    String getVersion();

    List<String> createInstallScripts();

    /**
     * Compares this runtime with another runtime  and returns any mismatches.
     * The comparison is assymetric: dependencies in requested runtime must all be present in this runtime,
     * but not the other way around.
     * @param requested runtime to compare with.
     * @return null if this runtime matches the requested.
     * A string describing the mismatches otherwise.
     */
    String getMismatches(ExecutionRuntime requested);
}

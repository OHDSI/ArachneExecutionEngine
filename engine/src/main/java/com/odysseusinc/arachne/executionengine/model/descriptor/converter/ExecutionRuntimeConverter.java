package com.odysseusinc.arachne.executionengine.model.descriptor.converter;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.ExecutionRuntimeDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;

public interface ExecutionRuntimeConverter<T extends ExecutionRuntimeDTO, V extends ExecutionRuntime> {
    T toDto(V model);
}

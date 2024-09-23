package com.odysseusinc.arachne.executionengine.model.descriptor.converter;

import com.google.common.collect.ImmutableMap;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.ExecutionRuntimeDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.TarballEnvironmentDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.converter.r.RExecutionRuntimeConverter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DescriptorConverter {
    private static final Map<RuntimeType, ExecutionRuntimeConverter<?, ?>> STRATEGIES = ImmutableMap.of(
            RuntimeType.R, new RExecutionRuntimeConverter()
    );

    public static TarballEnvironmentDTO toDto(Descriptor model) {
        TarballEnvironmentDTO dto = new TarballEnvironmentDTO();
        dto.setId(model.getId());
        dto.setBundleName(model.getBundleName());
        dto.setLabel(model.getLabel());
        dto.setExecutionRuntimes(
                Optional.ofNullable(model.getExecutionRuntimes()).map(runtime ->
                        runtime.stream().map(DescriptorConverter::convert).collect(Collectors.toList())
                ).orElseGet(Collections::emptyList)
        );
        return dto;
    }

    @SuppressWarnings("unchecked")
    private static <T extends ExecutionRuntimeDTO, V extends ExecutionRuntime> ExecutionRuntimeDTO convert(V executionRuntime) {
        return ((ExecutionRuntimeConverter<T, V>) STRATEGIES.get(executionRuntime.getType())).toDto(executionRuntime);
    }
}

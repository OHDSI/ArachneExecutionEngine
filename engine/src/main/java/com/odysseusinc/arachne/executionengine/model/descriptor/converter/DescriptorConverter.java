package com.odysseusinc.arachne.executionengine.model.descriptor.converter;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.ExecutionRuntimeDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.RuntimeEnvironmentDescriptorDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import java.util.List;
import java.util.stream.Collectors;

public class DescriptorConverter {
    private static final ExecutionRuntimeConverterHelper runtimeConverterHelper = new ExecutionRuntimeConverterHelper();

    public RuntimeEnvironmentDescriptorDTO toDto(Descriptor model) {
        RuntimeEnvironmentDescriptorDTO dto = new RuntimeEnvironmentDescriptorDTO();
        dto.setId(model.getId());
        dto.setBundleName(model.getBundleName());
        dto.setLabel(model.getLabel());
        List<ExecutionRuntimeDTO> executionRuntimes = model.getExecutionRuntimes().stream()
                .map(executionRuntime -> {
                    ExecutionRuntimeConverter converter = runtimeConverterHelper.getConverter(executionRuntime.getType());
                    return converter.toDto(executionRuntime);
                })
                .collect(Collectors.toList());
        dto.setExecutionRuntimes(executionRuntimes);
        return dto;
    }
}

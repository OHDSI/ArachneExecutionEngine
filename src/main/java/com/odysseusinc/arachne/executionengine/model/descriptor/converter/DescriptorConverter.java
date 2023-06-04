package com.odysseusinc.arachne.executionengine.model.descriptor.converter;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.DescriptorDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.ExecutionRuntimeDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;

import java.util.Set;
import java.util.stream.Collectors;

public class DescriptorConverter {
    private static final ExecutionRuntimeConverterHelper runtimeConverterHelper = new ExecutionRuntimeConverterHelper();

    public DescriptorDTO toDto(Descriptor model) {
        DescriptorDTO dto = new DescriptorDTO();
        dto.setId(model.getId());
        dto.setBundleName(model.getBundleName());
        dto.setLabel(model.getLabel());
        Set<ExecutionRuntimeDTO> executionRuntimes = model.getExecutionRuntimes().stream()
                .map(executionRuntime -> {
                    ExecutionRuntimeConverter converter = runtimeConverterHelper.getConverter(executionRuntime.getRuntimeType());
                    return converter.toDto(executionRuntime);
                })
                .collect(Collectors.toSet());
        dto.setExecutionRuntimes(executionRuntimes);
        return dto;
    }
}

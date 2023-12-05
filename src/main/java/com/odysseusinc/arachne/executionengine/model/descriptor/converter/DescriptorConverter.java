package com.odysseusinc.arachne.executionengine.model.descriptor.converter;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.ExecutionRuntimeDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.RuntimeEnvironmentDescriptorDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DescriptorConverter {
    private static final ExecutionRuntimeConverterHelper runtimeConverterHelper = new ExecutionRuntimeConverterHelper();

    public RuntimeEnvironmentDescriptorDTO toDto(Descriptor model) {
        RuntimeEnvironmentDescriptorDTO dto = new RuntimeEnvironmentDescriptorDTO();
        dto.setId(model.getId());
        dto.setBundleName(model.getBundleName());
        dto.setLabel(model.getLabel());
        List<ExecutionRuntime> modelExecutionRuntimes = Arrays.asList(model.getExecutionRuntimes());
        List<ExecutionRuntimeDTO> executionRuntimes = modelExecutionRuntimes.stream()
                .map(executionRuntime -> {
                    ExecutionRuntimeConverter converter = runtimeConverterHelper.getConverter(executionRuntime.getType());
                    return converter.toDto(executionRuntime);
                })
                .collect(Collectors.toList());
        dto.setExecutionRuntimes(executionRuntimes);
        return dto;
    }
}

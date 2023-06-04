package com.odysseusinc.arachne.executionengine.model.descriptor.converter.r;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.r.RDependencyDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.r.RExecutionRuntimeDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.converter.ExecutionRuntimeConverter;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;

import java.util.List;
import java.util.stream.Collectors;

public class RExecutionRuntimeConverter implements ExecutionRuntimeConverter<RExecutionRuntimeDTO, RExecutionRuntime> {
    private static final RDependencyConverter rDependencyConverter = new RDependencyConverter();

    @Override
    public RExecutionRuntimeDTO toDto(RExecutionRuntime model) {
        RExecutionRuntimeDTO dto = new RExecutionRuntimeDTO();
        dto.setVersion(model.getVersion());
        List<RDependencyDTO> dependencies = model.getDependencies().stream()
                .map(rDependency -> rDependencyConverter.toDto(rDependency))
                .collect(Collectors.toList());
        dto.setDependencies(dependencies);
        return dto;
    }
}

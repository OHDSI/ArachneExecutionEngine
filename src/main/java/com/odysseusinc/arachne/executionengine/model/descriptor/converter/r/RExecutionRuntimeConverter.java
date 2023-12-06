package com.odysseusinc.arachne.executionengine.model.descriptor.converter.r;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.r.RDependencyDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.r.RExecutionRuntimeDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.converter.ExecutionRuntimeConverter;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependency;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RExecutionRuntimeConverter implements ExecutionRuntimeConverter<RExecutionRuntimeDTO, RExecutionRuntime> {
    @Override
    public RExecutionRuntimeDTO toDto(RExecutionRuntime model) {
        RExecutionRuntimeDTO dto = new RExecutionRuntimeDTO();
        dto.setVersion(model.getVersion());
        List<RDependencyDTO> dependencies = Arrays.stream(model.getDependencies())
                .map(this::toRDependencyDto)
                .collect(Collectors.toList());
        dto.setDependencies(dependencies);
        return dto;
    }

    private RDependencyDTO toRDependencyDto(RDependency model) {
        RDependencyDTO dto = new RDependencyDTO();
        dto.setVersion(model.getVersion());
        dto.setName(model.getName());
        return dto;
    }
}

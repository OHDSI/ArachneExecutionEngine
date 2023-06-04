package com.odysseusinc.arachne.executionengine.model.descriptor.converter.r;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.r.RDependencyDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependency;

public class RDependencyConverter {
    public RDependencyDTO toDto(RDependency model) {
        RDependencyDTO dto = new RDependencyDTO();
        dto.setVersion(model.getVersion());
        dto.setName(model.getName());
        return dto;
    }
}

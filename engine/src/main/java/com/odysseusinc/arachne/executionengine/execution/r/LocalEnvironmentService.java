package com.odysseusinc.arachne.executionengine.execution.r;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.LocalEnvironmentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LocalEnvironmentService  {
    @Value("${runtime.local:false}")
    private boolean useLocalREnv;

    public List<LocalEnvironmentDTO> getEnvironments() {
        return useLocalREnv ? List.of(new LocalEnvironmentDTO()) : List.of();
    }

}

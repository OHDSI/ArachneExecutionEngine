package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.DockerEnvironmentDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.TarballEnvironmentDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EngineStatus {
    /**
     * The moment when EE has started.
     */
    private Instant started;
    /**
     * The list of
     */
    private Map<Long, ExecutionOutcome> submissions;

    private EngineStatus.Environments environments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Environments {
        private List<TarballEnvironmentDTO> tarball;
        private List<DockerEnvironmentDTO> docker;
    }
}

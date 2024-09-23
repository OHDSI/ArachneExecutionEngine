package com.odysseusinc.arachne.execution_engine_common.descriptor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TarballEnvironmentDTO {
    private String id;
    private String bundleName;
    private String label;
    private List<ExecutionRuntimeDTO> executionRuntimes;
}

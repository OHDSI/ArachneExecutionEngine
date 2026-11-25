package com.odysseusinc.arachne.execution_engine_common.descriptor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalEnvironmentDTO {
    // TODO add data describing the local environment
}

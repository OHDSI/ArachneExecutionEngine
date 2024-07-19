package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}

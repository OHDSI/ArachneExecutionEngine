package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@Setter
@Getter
public class AnalysisSyncRequestDTO {

    @NotNull(message = "id can not be null")
    @Min(value = 0, message = "id can not be below 0")
    private Long id;

    @NotNull
    private String executableFileName;

    @NotNull
    private DataSourceUnsecuredDTO dataSource;

    @NotNull
    private Date requested;

    private String requestedDescriptorId;

    private String resultExclusions = "";

    private String dockerImage;

    /**
     * Additional environment variables to be passed to execution.
     */
    private Map<String, String> parameters;
}

package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;

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

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getExecutableFileName() {

        return executableFileName;
    }

    public void setExecutableFileName(String executableFileName) {

        this.executableFileName = executableFileName;
    }

    public DataSourceUnsecuredDTO getDataSource() {

        return dataSource;
    }

    public void setDataSource(DataSourceUnsecuredDTO dataSource) {

        this.dataSource = dataSource;
    }

    public Date getRequested() {

        return requested;
    }

    public void setRequested(Date requested) {

        this.requested = requested;
    }

    public String getResultExclusions() {

        return resultExclusions;
    }

    public void setResultExclusions(String resultExclusions) {

        this.resultExclusions = resultExclusions;
    }

    public String getRequestedDescriptorId() {
        return requestedDescriptorId;
    }

    public void setRequestedDescriptorId(String requestedDescriptorId) {
        this.requestedDescriptorId = requestedDescriptorId;
    }
}

/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: February 01, 2017
 *
 */

package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.concurrent.Future;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisRequestStatusDTO {
    private Long id;
    private AnalysisRequestTypeDTO type;
    @JsonIgnore
    private Future executionFuture;
    private String actualDescriptorId;

    public AnalysisRequestStatusDTO() {

    }

    public AnalysisRequestStatusDTO(Long id, AnalysisRequestTypeDTO type) {

        this.id = id;
        this.type = type;
    }

    public AnalysisRequestStatusDTO(Long id, AnalysisRequestTypeDTO type, Future executionFuture) {

        this(id, type);
        this.executionFuture = executionFuture;
    }

    public AnalysisRequestStatusDTO(Long id, AnalysisRequestTypeDTO type, Future executionFuture, String actualDescriptorId) {

        this(id, type, executionFuture);
        this.actualDescriptorId = actualDescriptorId;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public AnalysisRequestTypeDTO getType() {

        return type;
    }

    public void setType(AnalysisRequestTypeDTO type) {

        this.type = type;
    }

    public Future getExecutionFuture() {

        return executionFuture;
    }

    public void setExecutionFuture(Future executionFuture) {

        this.executionFuture = executionFuture;
    }

    public String getActualDescriptorId() {
        return actualDescriptorId;
    }

    public void setActualDescriptorId(String actualDescriptorId) {
        this.actualDescriptorId = actualDescriptorId;
    }
}

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
 * Created: January 31, 2017
 *
 */

package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

import java.util.Date;

public class AnalysisExecutionStatusDTO {

    private Long id;
    private String stdout;
    private Date stdoutDate;

    public AnalysisExecutionStatusDTO() {
    }

    public AnalysisExecutionStatusDTO(Long id, String stdout, Date stdoutDate) {
        this.id = id;
        this.stdout = stdout;
        this.stdoutDate = stdoutDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public Date getStdoutDate() {
        return stdoutDate;
    }

    public void setStdoutDate(Date stdoutDate) {
        this.stdoutDate = stdoutDate;
    }
}

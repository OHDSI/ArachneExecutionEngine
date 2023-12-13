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

public enum AnalysisRequestTypeDTO {
    SQL("SQL-ANALYSE"),
    R("R-CONTAINER"),
    NOT_RECOGNIZED("NOT_RECOGNIZED");

    private String title;

    AnalysisRequestTypeDTO(String title) {

        this.title = title;
    }

    public String getTitle() {

        return title;
    }
}

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnalysisResultDTO {
    /**
     * Execution id, as provided in the request
     */
    private Long id;
    /**
     * The stage when the execution has concluded. See {@link Stage} for list of possible stages.
     * Consumers MUST NOT assume this is a closed list, new falues might be added in the future.
     */
    private String stage;
    /**
     * Error, if any, otherwise null. Note that even terminal stage can have an error.
     * A COMPLETED execution might still have an error when packing the results or doing some
     * post-processing, such as cleanup. No assumption should be made by consumers.
     */
    private String error;
    private String stdout;
    /**
     * @deprecated Use {@link #stage} and {@link #error} to inspect status detail
     */
    private AnalysisResultStatusDTO status;
    private Date requested;

}

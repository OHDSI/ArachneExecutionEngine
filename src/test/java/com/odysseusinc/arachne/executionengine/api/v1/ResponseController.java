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
 * Created: April 03, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.api.v1;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisExecutionStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ResponseController {

    @RequestMapping(value = "/submissions/{id}/update/{password}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void updateSubmission(@PathVariable Long id,
                                 @PathVariable String password,
                                 @RequestBody AnalysisExecutionStatusDTO status) {

        if (!AnalysysControllerTest.updateStatusIsOk.get()) {
            AnalysysControllerTest.updateStatusIsOk.set(true);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AnalysysControllerTest.latch.countDown();
        }
    }

    @RequestMapping(value = "/submissions/{id}/result/{password}",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void analysisResult(@PathVariable Long id,
                               @PathVariable String password,
                               @RequestPart("analysisResult") AnalysisResultDTO result,
                               @RequestPart("file") MultipartFile[] files) {

        if (!AnalysysControllerTest.resultIsOk.get()) {
            switch (id.intValue()) {
                case 1: {
                    AnalysysControllerTest.resultIsOk.set(files.length == 15);
                    break;
                }
                case 2: {
                    AnalysysControllerTest.resultIsOk.set(files.length == 6);
                    break;
                }
                default: {
                    AnalysysControllerTest.resultIsOk.set(false);
                    break;
                }
            }
            AnalysysControllerTest.latch.countDown();
        }
    }
}

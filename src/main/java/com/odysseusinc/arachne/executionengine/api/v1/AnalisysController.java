/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
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

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.executionengine.service.AnalysisService;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.lingala.zip4j.exception.ZipException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;


@RestController
@Api
@RequestMapping(value = AnalisysController.REST_API_MAIN)
public class AnalisysController {

    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_MAIN = "/api/v1";
    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_ANALYZE = "/analyze";
    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_METRICS = "/metrics";

    public static final String REST_API_THREAD = "/thread";

    private final AnalysisService analysisService;
    private final CallbackService callbackService;
    private final ThreadPoolTaskExecutor threadPoolExecutor;

    @Autowired
    public AnalisysController(AnalysisService analysisService, CallbackService callbackService,
                              ThreadPoolTaskExecutor threadPoolExecutor) {

        this.analysisService = analysisService;
        this.callbackService = callbackService;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @ApiOperation(value = "Files for analysis")
    @RequestMapping(value = REST_API_ANALYZE,
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public AnalysisRequestStatusDTO analyze(
            @RequestPart("analysisRequest") @Valid AnalysisRequestDTO analysisRequest,
            @RequestPart("file") List<MultipartFile> files,
            @RequestHeader(value = "arachne-compressed", defaultValue = "false") Boolean compressed,
            @RequestHeader(value = "arachne-waiting-compressed-result", defaultValue = "false") Boolean waitCompressedResult,
            @RequestHeader(value = "arachne-datasource-check", defaultValue = "false") Boolean healthCheck,
            @RequestHeader(value = "arachne-result-chunk-size-mb", defaultValue = "10485760") Long chunkSize
    ) throws IOException, ZipException {

        try {
            final File analysisDir = AnalisysUtils.extractFiles(files, compressed);
            return analysisService.analyze(analysisRequest, analysisDir, waitCompressedResult, healthCheck, chunkSize);
        } catch (IOException | ZipException e) {
            callbackService.sendFailedResult(analysisRequest, e, null, waitCompressedResult, chunkSize);
            throw e;
        }
    }

    @ApiOperation(value = "Prometheus compatible metrics")
    @RequestMapping(value = REST_API_METRICS,
            method = RequestMethod.GET,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String metrics() {

        int busy = analysisService.activeTasks();
        return "busy " + busy;
    }

    @RequestMapping(value = REST_API_THREAD, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String thread() {

        StringBuilder result = new StringBuilder();
        result.append("Queue: ").append(threadPoolExecutor.getThreadPoolExecutor().getQueue().size()).append("\n");
        result.append("Core pool size: ").append(threadPoolExecutor.getCorePoolSize()).append("\n");
        result.append("Pool size: ").append(threadPoolExecutor.getPoolSize()).append("\n");
        result.append("Max pool size: ").append(threadPoolExecutor.getMaxPoolSize()).append("\n");
        result.append("Active: ").append(threadPoolExecutor.getActiveCount()).append("\n");
        result.append("Task completed: ").append(threadPoolExecutor.getThreadPoolExecutor().getCompletedTaskCount()).append("\n");
        return result.toString();
    }

}

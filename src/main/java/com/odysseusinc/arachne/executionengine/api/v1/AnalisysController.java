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

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.client.FeignSpringFormEncoder;
import com.odysseusinc.arachne.executionengine.service.AnalysisService;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.service.impl.StdoutHandlerParams;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;


@RestController
@Api
@RequestMapping(value = AnalisysController.REST_API_MAIN)
public class AnalisysController {

    private static final Logger log = LoggerFactory.getLogger(AnalisysController.class);

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
            @RequestHeader(value = "arachne-attach-cdm-metadata", defaultValue = "true") Boolean attachCdmMetadata,
            @RequestHeader(value = "arachne-result-chunk-size-mb", defaultValue = "10485760") Long chunkSize
    ) throws IOException {

        try {
            log.info("Started processing request for analysis ID = {}", analysisRequest.getId());
            final File analysisDir = AnalisysUtils.extractFiles(files, compressed);
            log.info("Extracted files for analysis ID = {}", analysisRequest.getId());
            return analysisService.analyze(analysisRequest, analysisDir, waitCompressedResult, attachCdmMetadata, chunkSize);
        } catch (IOException e) {
            callbackService.sendFailedResult(analysisRequest, e, null, waitCompressedResult, chunkSize);
            throw e;
        }
    }

    @ApiOperation(value = "Execute analysis synchronously")
    @RequestMapping(value = "/analyze/sync",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity analyzeSync(
            @RequestPart("analysisRequest") AnalysisSyncRequestDTO analysisRequest,
            @RequestPart("file") List<MultipartFile> files
    ) throws IOException, InterruptedException {

        log.info("Started processing request for synchronous analysis ID = {}", analysisRequest.getId());
        final File analysisDir = AnalisysUtils.extractFiles(files, false);
        log.info("Extracted files for synchronous analysis ID = {}", analysisRequest.getId());

        StringBuilder stdoutBuilder = new StringBuilder();
        StdoutHandlerParams stdoutHandlerParams = new StdoutHandlerParams(
                100,
                stdoutDiff -> stdoutBuilder.append(stdoutDiff).append("\r\n")
        );

        long startTime = System.currentTimeMillis();

        AnalysisRequestStatusDTO requestStatus = analysisService.analyze(
                analysisRequest,
                analysisDir,
                false,
                stdoutHandlerParams,
                (resultingStatus, stdout, resultDir, ex) -> {
                }
        );

        Future executionFuture = requestStatus.getExecutionFuture();
        while (!executionFuture.isDone()) {
            Thread.sleep(100);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        log.info("Execution of synchronous analysis ID = {} took: {} sec", analysisRequest.getId(), elapsedTime / 1000);

        final MultiValueMap<String, Object> results = new LinkedMultiValueMap<>();

        // Encode and attach result files
        File[] directoryListing = analysisDir.listFiles();
        if (directoryListing != null) {
            Arrays.stream(directoryListing).filter(File::isFile).forEach(f -> {
                try {
                    MultipartFile mf = new MockMultipartFile(f.getName(), f.getName(), null, new FileInputStream(f));
                    results.add("file", FeignSpringFormEncoder.encodeMultipartFile(mf));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        }

        // Encode and attach stdout
        results.add("stdout", FeignSpringFormEncoder.encodeMultipartFile(new MockMultipartFile("stdout.txt", "stdout.txt", null, stdoutBuilder.toString().getBytes())));

        // Encode and attach status DTO
        results.add("status", FeignSpringFormEncoder.encodeJsonObject(requestStatus));

        return new ResponseEntity<>(results, HttpStatus.OK);
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

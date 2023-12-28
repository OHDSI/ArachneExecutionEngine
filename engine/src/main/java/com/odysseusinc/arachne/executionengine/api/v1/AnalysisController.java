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

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.NOT_RECOGNIZED;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.executionengine.execution.AnalysisService;
import com.odysseusinc.arachne.executionengine.execution.CallbackService;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.validation.Valid;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@Api
@RequestMapping(value = AnalysisController.REST_API_MAIN)
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

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

    @Value("${analysis.dir:/etc/executions}")
    private String analysisParentDir;

    @Autowired
    public AnalysisController(AnalysisService analysisService, CallbackService callbackService,
                              ThreadPoolTaskExecutor threadPoolExecutor) {

        this.analysisService = analysisService;
        this.callbackService = callbackService;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @ApiOperation(value = "Files for analysis")
    @RequestMapping(value = REST_API_ANALYZE,
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )

    public AnalysisRequestStatusDTO analyze(
            @RequestPart("analysisRequest") @Valid AnalysisRequestDTO analysisRequest,
            @RequestPart("file") List<MultipartFile> files,
            @RequestHeader(value = "arachne-compressed", defaultValue = "false") Boolean compressed,
            @RequestHeader(value = "arachne-waiting-compressed-result", defaultValue = "false") Boolean waitCompressedResult,
            @RequestHeader(value = "arachne-attach-cdm-metadata", defaultValue = "true") Boolean attachCdmMetadata,
            @RequestHeader(value = "arachne-result-chunk-size-mb", defaultValue = "10485760") Long chunkSize
    ) throws IOException {

        Long id = analysisRequest.getId();
        try {
            log.info("Request [{}] for [{}] received", id, analysisRequest.getResultCallback());
            File analysisDir = AnalisysUtils.extractFiles(files, analysisParentDir, compressed);
            Integer extracted = Optional.ofNullable(analysisDir.list()).map(dir -> dir.length).orElse(-1);
            log.info("Request [{}] extracted {} files to [{}]", id, extracted, analysisDir.getAbsolutePath());
            AnalysisRequestStatusDTO result = analysisService.analyze(analysisRequest, analysisDir, waitCompressedResult, attachCdmMetadata, chunkSize);
            log.info("Request [{}] of type [{}] accepted into processing", id, result.getType());
            return result;
        } catch (IOException e) {
            log.info("Request [{}] NOT accepted due to [{}]: {}", id, e.getClass().getName(), e.getMessage());
            AnalysisResultDTO result = analysisService.buildResult(analysisRequest, null, e);
            callbackService.sendResults(result, null, analysisRequest.getResultCallback(), analysisRequest.getCallbackPassword());
            log.info("Request [{}] completed: negative callback sent", id);
            throw e;
        }
    }

    @ApiOperation(value = "Execute analysis synchronously")
    @RequestMapping(value = "/analyze/sync",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MultiValueMap<String, Object>> analyzeSync(
            @RequestPart("analysisRequest") AnalysisSyncRequestDTO analysisRequest,
            @RequestPart("file") List<MultipartFile> files
    ) throws IOException {

        Long id = analysisRequest.getId();
        log.info("Started processing request for synchronous analysis ID = {}", id);
        final File analysisDir = AnalisysUtils.extractFiles(files, analysisParentDir, false);
        log.info("Extracted files for synchronous analysis ID = {}", id);

        StringBuilder stdoutBuilder = new StringBuilder();
        BiConsumer<String, String> callback = (stage, stdoutDiff) -> stdoutBuilder.append(stdoutDiff).append("\r\n");

        long startTime = System.currentTimeMillis();

        try {
            AnalysisRequestStatusDTO stat = analysisService.analyze(analysisRequest, analysisDir, false, callback, 100).map(overseer -> {
                overseer.getResult().join();
                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("Execution of synchronous analysis ID = {} took: {} sec", id, elapsedTime / 1000);
                return new AnalysisRequestStatusDTO(id, overseer.getType(), overseer.getEnvironment());
            }).orElseGet(() ->
                    new AnalysisRequestStatusDTO(id, NOT_RECOGNIZED, null)
            );
            return results(analysisDir, stdoutBuilder, stat);
        } catch (Throwable e) {
            log.info("Execution [{}] init failed: ", id, e);
            AnalysisRequestStatusDTO status = new AnalysisRequestStatusDTO(id, NOT_RECOGNIZED, ExceptionUtils.getStackTrace(e));
            return results(analysisDir, stdoutBuilder, status);
        }

    }

    private ResponseEntity<MultiValueMap<String, Object>> results(
            File analysisDir, StringBuilder stdoutBuilder, AnalysisRequestStatusDTO status
    ) throws IOException {
        final MultiValueMap<String, Object> results = new LinkedMultiValueMap<>();

        // Encode and attach result files
        File[] directoryListing = analysisDir.listFiles();
        if (directoryListing != null) {
            Arrays.stream(directoryListing).filter(File::isFile).forEach(f -> {
                try {
                    MultipartFile mf = new MockMultipartFile(f.getName(), f.getName(), null, new FileInputStream(f));
                    results.add("file", encodeMultipartFile(mf));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        }

        // Encode and attach stdout
        results.add("stdout", encodeMultipartFile(new MockMultipartFile("stdout.txt", "stdout.txt", null, stdoutBuilder.toString().getBytes())));

        // Encode and attach status DTO
        results.add("status", encodeJsonObject(status));
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @ApiOperation(value = "Abort running analysis job")
    @PostMapping(value = "/abort/{id}")
    public ResponseEntity<AnalysisResultDTO> cancel(
            @PathVariable("id") String analysisId
    ) {
        Long id = Long.parseLong(analysisId);
        return analysisService.abort(id).map(ResponseEntity::ok).orElseGet(() ->
                ResponseEntity.notFound().build()
        );
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
    
    /**
     * Wraps a single {@link MultipartFile} into a {@link HttpEntity} and sets the
     * {@code Content-type} header to {@code application/octet-stream}
     *
     * @param file
     * @return
     */
    private HttpEntity<Resource> encodeMultipartFile(MultipartFile file) throws IOException {
        HttpHeaders filePartHeaders = new HttpHeaders();
        filePartHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        Resource multipartFileResource = new MultipartFileResource(file.getOriginalFilename(), file.getSize(), file.getInputStream());
        return new HttpEntity<>(multipartFileResource, filePartHeaders);
    }
    
    /**
     * Wraps an object into a {@link HttpEntity} and sets the {@code Content-type} header to
     * {@code application/json}
     *
     * @param o
     * @return
     */
    private HttpEntity<?> encodeJsonObject(Object o) {
        HttpHeaders jsonPartHeaders = new HttpHeaders();
        jsonPartHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(o, jsonPartHeaders);
    }
    
    /**
     * Dummy resource class. Wraps file content and its original name.
     */
    static class MultipartFileResource extends InputStreamResource {

        private final String filename;
        private final long size;

        public MultipartFileResource(String filename, long size, InputStream inputStream) {
            super(inputStream);
            this.size = size;
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }

        @Override
        public InputStream getInputStream() throws IOException, IllegalStateException {
            return super.getInputStream();
        }

        @Override
        public long contentLength() throws IOException {
            return size;
        }

    }


}

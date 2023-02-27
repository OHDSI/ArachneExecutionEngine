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
 * Created: March 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.service.impl;

import com.google.common.io.Files;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisExecutionStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CallbackServiceImpl implements CallbackService {
    private static final Logger log = LoggerFactory.getLogger(CallbackServiceImpl.class);
    private final Map<Long, Date> outSented = new ConcurrentHashMap<>();
    @Value("${submission.update.interval}")
    private Long submissionUpdateInterval;
    @Value("${submission.cleanupResults}")
    private boolean cleanupResults;
    private final RestTemplate nodeRestTemplate;
    private final RetryTemplate successfulRetryTemplate;
    private final RetryTemplate failureRetryTemplate;
    private static final String SENDING_STDOUT_TO_CENTRAL_LOG =
            "Sending stdout to callback for analysis with id='{}'";
    private static final String UPDATE_STATUS_FAILED_LOG = "Update analysis status id={} failed";
    private static final String SEND_RESULT_FAILED_LOG = "Send analysis result id={} failed";
    private static final String SEND_ERROR_RESULT_FAILED_LOG = "Send error analysis result id={} failed";
    private static final String EXECUTION_RESULT_FILES_COUNT_LOG = "Execution id={} produced {} result files";
    private static final String DELETE_DIR_ERROR_LOG = "Can't delete analysis directory: '{}'";

    @Autowired
    public CallbackServiceImpl(@Qualifier("nodeRestTemplate") RestTemplate nodeRestTemplate,
                               @Qualifier("successCallbackRetryTemplate") RetryTemplate successfulRetryTemplate,
                               @Qualifier("failureCallbackRetryTemplate") RetryTemplate failureRetryTemplate) {

        this.nodeRestTemplate = nodeRestTemplate;
        this.successfulRetryTemplate = successfulRetryTemplate;
        this.failureRetryTemplate = failureRetryTemplate;
    }

    @Override
    @Async
    @FileDescriptorCount
    public void updateAnalysisStatus(String updateURL, Long submissionId, String out, String password) {

        Date current = new Date();
        Date sentedAt = outSented.get(submissionId);
        if (sentedAt == null) {
            sentedAt = new Date(current.getTime() - submissionUpdateInterval - 1);
            outSented.put(submissionId, current);
        }
        if ((sentedAt.getTime() + submissionUpdateInterval) < current.getTime()) {
            log.info(SENDING_STDOUT_TO_CENTRAL_LOG, submissionId);
            AnalysisExecutionStatusDTO status = new AnalysisExecutionStatusDTO(submissionId, out, current);
            HttpEntity<AnalysisExecutionStatusDTO> entity = new HttpEntity<>(status);
            try {
                nodeRestTemplate.exchange(
                        updateURL,
                        HttpMethod.POST,
                        entity,
                        String.class,
                        submissionId,
                        password);
            } catch (RestClientException ex) {
                log.info(UPDATE_STATUS_FAILED_LOG, submissionId, ex);
            }
            outSented.replace(submissionId, current);
        }
    }

    @Override
    @FileDescriptorCount
    public void processAnalysisResult(
            AnalysisRequestDTO analysis,
            AnalysisResultStatusDTO status,
            String stdout,
            File resultDir,
            Boolean compressedResult,
            Long chunkSize
    ) throws ZipException {

        final File zipDir = Files.createTempDir();
        try {
            AnalysisResultDTO result = new AnalysisResultDTO();
            result.setId(analysis.getId());
            result.setRequested(analysis.getRequested());
            result.setStdout(stdout);
            result.setStatus(status);

            int resultFilesCnt = AnalisysUtils.getDirectoryItems(resultDir).size();
            log.info(EXECUTION_RESULT_FILES_COUNT_LOG, analysis.getId(), resultFilesCnt);

            final List<FileSystemResource> resultFSResources
                    = AnalisysUtils.getFileSystemResources(analysis, resultDir, compressedResult, chunkSize, zipDir);

            sendAnalysisResult(analysis, result, resultFSResources, chunkSize);
        } catch (ZipException ex) {
            log.error(ex.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Stacktrace: ", ex);
            }
            throw ex;
        } finally {
            try {
                if (cleanupResults) {
                    FileUtils.deleteDirectory(resultDir);
                    FileUtils.deleteQuietly(zipDir);
                }
            } catch (IOException ex) {
                log.warn(DELETE_DIR_ERROR_LOG, resultDir.getAbsolutePath(), ex);
                // rethrow of exception cause to results overriden
//                throw ex;
            }
        }
    }

    @Override
    @FileDescriptorCount
    public void sendAnalysisResult(AnalysisRequestDTO analysis,
                                   AnalysisResultDTO analysisResult,
                                   Collection<FileSystemResource> files,
                                   Long chunkSize) {
        successfulRetryTemplate.execute(
                (RetryCallback<ResponseEntity<String>, RestClientException>) retryContext -> executeSend(analysis, analysisResult, files),
                retryContext -> sendFailedResult(analysis, retryContext.getLastThrowable(), null, false, chunkSize)
        );
    }
    
    @Override
    @FileDescriptorCount
    public ResponseEntity<String> sendFailedResult(AnalysisRequestDTO analysis, Throwable e, File analysisDir,
                                                   Boolean compressedResult, Long chunkSize) {
        final String stdout = getErrorStackTrace(e);
        return failureRetryTemplate.execute(retryContext -> {
            AnalysisResultDTO result = new AnalysisResultDTO();
            result.setId(analysis.getId());
            result.setStatus(AnalysisResultStatusDTO.FAILED);
            result.setRequested(analysis.getRequested());
            List<FileSystemResource> resultFSResources = null;
            result.setStdout(stdout);
            try {
                if (Objects.nonNull(analysisDir)) {
                    resultFSResources = AnalisysUtils.getFileSystemResources(analysis, analysisDir, compressedResult, chunkSize,
                            Files.createTempDir());
                }
            } catch (ZipException ex) {
                log.error("could not collect analysis results, id={}", analysis.getId());
                result.setStdout(stdout + "\n" + ExceptionUtils.getStackTrace(ex));
            }

            return executeSend(analysis, result, resultFSResources);
        });
    }
    
    private String getErrorStackTrace(Throwable e) {
        if (Objects.nonNull(e)) {
            return ExceptionUtils.getStackTrace(e);
        } else {
            return "";
        }
    }
    private ResponseEntity<String> executeSend(AnalysisRequestDTO analysis, AnalysisResultDTO analysisResult, Collection<FileSystemResource> files) {
        HttpHeaders jsonHeader = new HttpHeaders();
        jsonHeader.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AnalysisResultDTO> analysisRequestHttpEntity = new HttpEntity<>(analysisResult, jsonHeader);
        LinkedMultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
        multipartRequest.add("analysisResult", analysisRequestHttpEntity);
        if (files != null) {
            files.forEach(f -> multipartRequest.add("file", f));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(multipartRequest, headers);
        Long submissionId = analysisResult.getId();
        return nodeRestTemplate.exchange(
                analysis.getResultCallback(),
                HttpMethod.POST,
                entity,
                String.class,
                submissionId,
                analysis.getCallbackPassword());
    }
}

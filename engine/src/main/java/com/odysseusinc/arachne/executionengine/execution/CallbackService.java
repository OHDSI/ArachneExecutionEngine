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

package com.odysseusinc.arachne.executionengine.execution;

import com.google.common.io.Files;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisExecutionStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import com.odysseusinc.arachne.executionengine.util.AutoCloseWrapper;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CallbackService {
    private static final Logger log = LoggerFactory.getLogger(CallbackService.class);
    @Value("${submission.cleanupResults}")
    private boolean cleanupResults;
    @Autowired
    @Qualifier("nodeRestTemplate")
    private RestTemplate nodeRestTemplate;
    @Autowired
    @Qualifier("successCallbackRetryTemplate")
    private RetryTemplate successfulRetryTemplate;

    @Async
    @FileDescriptorCount
    public void updateAnalysisStatus(AnalysisRequestDTO analysis, String stage, String logs) {
        Long id = analysis.getId();
        log.info("Execution [{}] sending status [{}], {} chars of log", id, stage, logs.length());
        AnalysisExecutionStatusDTO status = new AnalysisExecutionStatusDTO(id, stage, logs, new Date());
        HttpEntity<AnalysisExecutionStatusDTO> entity = new HttpEntity<>(status);
        String url = analysis.getUpdateStatusCallback();
        try {
            nodeRestTemplate.exchange(url, HttpMethod.POST, entity, String.class, id, analysis.getCallbackPassword());
        } catch (RestClientException ex) {
            log.error("Execution [{}] send status to [{}] failed: {}", id, url, ex.getMessage());
        }
    }

    public AutoCloseWrapper<List<FileSystemResource>> packResults(
            AnalysisRequestDTO analysis, File resultDir, Boolean compressedResult, Long chunkSize
    ) throws ZipException {
        Long id = analysis.getId();
        File zipDir = Files.createTempDir();
        Runnable cleanup = cleanupResults ? () -> Stream.of(resultDir, zipDir).forEach(FileUtils::deleteQuietly) : () -> {};
        try {
            int resultFilesCnt = AnalisysUtils.getDirectoryItems(resultDir).size();
            log.info("Execution [{}] produced {} result files", id, resultFilesCnt);

            List<File> files = compressedResult
                    ? getCompressedResults(analysis, resultDir, chunkSize, id, zipDir)
                    : AnalisysUtils.getDirectoryItemsExclude(resultDir, AnalisysUtils.EXCLUDE_JARS_MATCHER);
            return AutoCloseWrapper.of(CommonFileUtils.getFSResources(files), cleanup);
        } catch (RuntimeException | ZipException e) {
            cleanup.run();
            throw e;
        }
    }

    private List<File> getCompressedResults(AnalysisRequestDTO analysis, File resultDir, Long chunkSize, Long id, File zipDir) throws ZipException {
        final File archive = new File(zipDir, id + "_result.zip");
        log.info("Adding folder [{}] to zip [{}] with chunk size = {}", resultDir.getAbsolutePath(), archive.getAbsolutePath(), chunkSize);
        try {
            final File dir = CommonFileUtils.compressAndSplit(resultDir, archive, chunkSize, analysis.getResultExclusions());
            return AnalisysUtils.getDirectoryItemsExclude(dir, AnalisysUtils.EXCLUDE_JARS_MATCHER);
        } catch (ZipException ex) {
            log.error(ex.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Stacktrace: ", ex);
            }
            throw ex;
        }
    }

    public void sendResults(AnalysisResultDTO result, Collection<FileSystemResource> files, String url, String password) {
        Long id = result.getId();
        successfulRetryTemplate.execute(
                ctx -> {
                    Throwable t = ctx.getLastThrowable();
                    if (t == null) {
                        log.warn("Execution [{}] send result: {} - {}", id, result.getStage(), result.getError());
                    } else {
                        log.info("Execution [{}] retry send result after error: {}", id, t.getMessage());
                    }
                    ResponseEntity<String> sent = executeSend(result, files, url, password);
                    log.info("Execution [{}] result status sent, response HTTP {}", id, sent.getStatusCode());
                    return sent;
                },
                ctx -> {
                    log.error("Execution [{}] failed to send results: {}", id, ctx.getLastThrowable().getMessage());
                    return null;
                }
        );
    }

    private ResponseEntity<String> executeSend(AnalysisResultDTO analysisResult, Collection<FileSystemResource> files, String url, String password) {
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
        return nodeRestTemplate.exchange(url, HttpMethod.POST, entity, String.class, submissionId, password);
    }
}

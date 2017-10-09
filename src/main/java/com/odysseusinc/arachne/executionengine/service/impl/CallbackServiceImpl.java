/**
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
 * Created: August 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.service.impl;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisExecutionStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CallbackServiceImpl implements CallbackService {
    private static final Logger log = LoggerFactory.getLogger(CallbackServiceImpl.class);
    private final Map<Long, Date> outSented = new ConcurrentHashMap<>();
    @Value("${submission.update.interval}")
    private Long submissionUpdateInterval;
    private final RestTemplate nodeRestTemplate;
    private static final String SENDING_STDOUT_TO_CENTRAL_LOG =
            "Sending stdout to callback for analysis with id='{}'";
    private static final String UPDATE_STATUS_FAILED_LOG = "Update analysis status id={} failed";
    private static final String SEND_RESULT_FAILED_LOG = "Send analysis result id={} failed";

    @Autowired
    public CallbackServiceImpl(@Qualifier("nodeRestTemplate") RestTemplate nodeRestTemplate) {

        this.nodeRestTemplate = nodeRestTemplate;
    }

    @Override
    @Async
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
    public void sendAnalysisResult(String resultURL,
                                   String password,
                                   AnalysisResultDTO analysisResult,
                                   Collection<FileSystemResource> files) {

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
        try {
            nodeRestTemplate.exchange(
                    resultURL,
                    HttpMethod.POST,
                    entity,
                    String.class,
                    submissionId,
                    password);
        } catch (RestClientException ex) {
            log.info(SEND_RESULT_FAILED_LOG, submissionId, ex);
        }
    }
}

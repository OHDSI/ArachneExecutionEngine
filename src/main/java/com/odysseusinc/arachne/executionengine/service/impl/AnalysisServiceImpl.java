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
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.service.AnalysisService;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.service.CdmMetadataService;
import com.odysseusinc.arachne.executionengine.service.KerberosService;
import com.odysseusinc.arachne.executionengine.service.RuntimeService;
import com.odysseusinc.arachne.executionengine.service.SQLService;
import com.odysseusinc.arachne.executionengine.util.FailedCallback;
import com.odysseusinc.arachne.executionengine.util.ResultCallback;
import java.io.File;
import java.util.Map;
import javafx.util.Pair;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class AnalysisServiceImpl implements AnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);

    private final SQLService sqlService;
    private final RuntimeService runtimeService;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final CdmMetadataService cdmMetadataService;
    private final CallbackService callbackService;
    private final KerberosService kerberosService;

    @Autowired
    public AnalysisServiceImpl(SQLService sqlService,
                               RuntimeService runtimeService,
                               @Qualifier("analysisTaskExecutor") ThreadPoolTaskExecutor threadPoolTaskExecutor,
                               CdmMetadataService cdmMetadataService,
                               CallbackService callbackService,
                               KerberosService kerberosService) {

        this.sqlService = sqlService;
        this.runtimeService = runtimeService;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.cdmMetadataService = cdmMetadataService;
        this.callbackService = callbackService;
        this.kerberosService = kerberosService;
    }

    @Override
    @FileDescriptorCount
    public AnalysisRequestStatusDTO analyze(AnalysisRequestDTO analysis, File analysisDir, Boolean compressedResult,
                                            Boolean attachCdmMetadata, Long chunkSize) {

        Validate.notNull(analysis, "analysis can't be null");

        AnalysisRequestTypeDTO status = AnalysisRequestTypeDTO.NOT_RECOGNIZED;
        try {
            Pair<Map<String, String>, String[]> krbPair = kerberosService.prepareToKinit(analysis.getDataSource(), analysisDir, runtimeService.getRuntimeServiceMode());
            Map<String, String> krbEnvProps = krbPair.getKey();
            if (runtimeService.getRuntimeServiceMode() == RuntimeServiceImpl.RuntimeServiceMode.SINGLE) {
                kerberosService.runKinit(analysisDir, krbPair.getValue());
            }

            if (attachCdmMetadata) {
                try {
                    cdmMetadataService.extractMetadata(analysis, analysisDir);
                } catch (Exception e) {
                    logger.info("Failed to collect CDM metadata. " + e);
                }
            }
            String executableFileName = analysis.getExecutableFileName();
            String fileExtension = Files.getFileExtension(executableFileName).toLowerCase();

            ResultCallback resultCallback = (finishedAnalysis, resultStatus, stdout, resultDir) ->
                    callbackService.processAnalysisResult(finishedAnalysis, resultStatus, stdout, resultDir, compressedResult, chunkSize);
            FailedCallback failedCallback = (failedAnalysis, ex, resultDir) ->
                    callbackService.sendFailedResult(failedAnalysis, ex, resultDir, compressedResult, chunkSize);

            switch (fileExtension) {
                case "sql": {
                    sqlService.analyze(analysis, analysisDir, resultCallback, failedCallback);
                    logger.info("analysis with id={} started in SQL Service", analysis.getId());
                    status = AnalysisRequestTypeDTO.SQL;
                    break;
                }

                case "r": {
                    runtimeService.analyze(analysis, analysisDir, resultCallback, failedCallback, krbEnvProps);
                    kerberosService.removeTempFiles();
                    logger.info("analysis with id={} started in R Runtime Service", analysis.getId());
                    status = AnalysisRequestTypeDTO.R;
                    break;
                }

                default: {
                    logger.info("analysis with id={} is not recognized. Skipping", analysis.getId());
                    status = AnalysisRequestTypeDTO.NOT_RECOGNIZED;
                }
            }
        } catch (Throwable e) {
            logger.error("analysis with id={} failed to execute", analysis.getId(), e);
            callbackService.sendFailedResult(analysis, e, analysisDir, compressedResult, chunkSize);
        }
        return new AnalysisRequestStatusDTO(analysis.getId(), status);
    }

    @Override
    public int activeTasks() {

        return threadPoolTaskExecutor.getActiveCount();
    }
}

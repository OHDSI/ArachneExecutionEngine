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
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.service.AnalysisService;
import com.odysseusinc.arachne.executionengine.service.CallbackService;
import com.odysseusinc.arachne.executionengine.service.CdmMetadataService;
import com.odysseusinc.arachne.executionengine.service.RuntimeService;
import com.odysseusinc.arachne.executionengine.service.SQLService;
import com.odysseusinc.arachne.executionengine.util.AnalysisCallback;
import com.odysseusinc.datasourcemanager.jdbc.auth.BigQueryAuthResolver;
import com.odysseusinc.datasourcemanager.jdbc.auth.DataSourceAuthResolver;
import com.odysseusinc.datasourcemanager.jdbc.auth.KerberosAuthResolver;
import com.odysseusinc.datasourcemanager.krblogin.KerberosService;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AnalysisServiceImpl implements AnalysisService, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);

    private final SQLService sqlService;
    private final RuntimeService runtimeService;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final CdmMetadataService cdmMetadataService;
    private final CallbackService callbackService;
    private final KerberosService kerberosService;
    @Value("${drivers.location.impala}")
    private String impalaDriversLocation;
    @Value("${drivers.location.bq}")
    private String bqDriversLocation;
    @Value("${drivers.location.netezza}")
    private String netezzaDriversLocation;

    @Value("${submission.update.interval}")
    private int submissionUpdateInterval;

    private String driverPathExclusions;
    private List<DataSourceAuthResolver> authResolvers;

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
        initAuthResolvers();
    }

    private void initAuthResolvers() {

        this.authResolvers = new ArrayList<>();
        authResolvers.add(new BigQueryAuthResolver());
        authResolvers.add(new KerberosAuthResolver(kerberosService));
    }

    @Override
    public AnalysisRequestStatusDTO analyze(AnalysisSyncRequestDTO analysis, File analysisDir, Boolean attachCdmMetadata, StdoutHandlerParams stdoutHandlerParams, AnalysisCallback resultCallback) {

        Validate.notNull(analysis, "analysis can't be null");
        AnalysisRequestTypeDTO status = AnalysisRequestTypeDTO.NOT_RECOGNIZED;
        Future executionFuture = null;
        try {
            File keystoreDir = new File(analysisDir, "keys");
            keystoreDir.mkdirs();

            DataSourceUnsecuredDTO dataSourceData = analysis.getDataSource();
            List<Optional> results = authResolvers.stream().filter(r -> r.supports(dataSourceData))
                    .map(r -> r.resolveAuth(dataSourceData, keystoreDir))
                    .collect(Collectors.toList());
            KrbConfig krbConfig = new KrbConfig();
            for(Optional val : results){
                if (val.isPresent() && val.get() instanceof KrbConfig) {
                    krbConfig = (KrbConfig) val.get();
                    break;
                }
            }

            String executableFileName = analysis.getExecutableFileName();
            String fileExtension = Files.getFileExtension(executableFileName).toLowerCase();

            analysis.setResultExclusions(Stream.of(analysis.getResultExclusions(), driverPathExclusions)
                    .filter(StringUtils::isNotBlank).collect(Collectors.joining(",")));

            AnalysisCallback logCleanupCallback = (resultingStatus, stdout, resultDir, ex) -> {
                if (attachCdmMetadata) {
                    saveMetadata(analysis, resultDir);
                }
                // Keystore folder must be deleted before zipping results
                FileUtils.deleteQuietly(keystoreDir);
                resultCallback.execute(resultingStatus, stdout, resultDir, ex);
            };

            switch (fileExtension) {
                case "sql": {
                    executionFuture = sqlService.analyze(analysis, analysisDir, stdoutHandlerParams, logCleanupCallback);
                    logger.info("analysis with id={} started in SQL Service", analysis.getId());
                    status = AnalysisRequestTypeDTO.SQL;
                    break;
                }

                case "r": {
                    executionFuture = runtimeService.analyze(analysis, analysisDir, stdoutHandlerParams, logCleanupCallback, krbConfig);
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
            resultCallback.execute(null, null, analysisDir, e);
        }
        return new AnalysisRequestStatusDTO(analysis.getId(), status, executionFuture);
    }

    @Override
    @FileDescriptorCount
    public AnalysisRequestStatusDTO analyze(AnalysisRequestDTO analysis, File analysisDir, Boolean compressedResult,
                                            Boolean attachCdmMetadata, Long chunkSize) {

        AnalysisCallback resultCallback = (resultingStatus, stdout, resultDir, ex) -> {
            if (ex == null) {
                try {
                    callbackService.processAnalysisResult(analysis, resultingStatus, stdout, resultDir, compressedResult, chunkSize);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                callbackService.sendFailedResult(analysis, ex, resultDir, compressedResult, chunkSize);
            }
        };

        StdoutHandlerParams stdoutHandlerParams = new StdoutHandlerParams(
            submissionUpdateInterval,
            stdoutDiff -> callbackService.updateAnalysisStatus(analysis.getUpdateStatusCallback(), analysis.getId(), stdoutDiff, analysis.getCallbackPassword())
        );

        return analyze(analysis, analysisDir, attachCdmMetadata, stdoutHandlerParams, resultCallback);
    }

    @Override
    public int activeTasks() {

        return threadPoolTaskExecutor.getActiveCount();
    }

    private void saveMetadata(AnalysisSyncRequestDTO analysis, File toDir) {

        try {
            cdmMetadataService.extractMetadata(analysis.getDataSource(), toDir);
        } catch (Exception e) {
            logger.info("Failed to collect CDM metadata for analysis id={}. {}", analysis.getId(), e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        driverPathExclusions = Stream.of(impalaDriversLocation, bqDriversLocation, netezzaDriversLocation)
                .filter(StringUtils::isNotBlank)
                .map(path -> path.startsWith("/") ? path.substring(1) : path)
                .map(path -> path + "/**/*")
                .collect(Collectors.joining(","));
    }
}

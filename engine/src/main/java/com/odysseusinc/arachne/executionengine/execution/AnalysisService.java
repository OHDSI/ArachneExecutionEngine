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

import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.EngineStatus;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.EngineStatus.Environments;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.DockerEnvironmentDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.TarballEnvironmentDTO;
import com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount;
import com.odysseusinc.arachne.executionengine.auth.AuthEffects;
import com.odysseusinc.arachne.executionengine.auth.CredentialsProvider;
import com.odysseusinc.arachne.executionengine.execution.r.DockerEnvironmentService;
import com.odysseusinc.arachne.executionengine.model.descriptor.converter.DescriptorConverter;
import com.odysseusinc.arachne.executionengine.service.CdmMetadataService;
import com.odysseusinc.arachne.executionengine.service.impl.DescriptorServiceImpl;
import com.odysseusinc.arachne.executionengine.util.AutoCloseWrapper;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.NOT_RECOGNIZED;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@Slf4j
@Service
public class AnalysisService {
    private final Map<String, ExecutionService> executionServices;
    private final ConcurrentMap<Long, Overseer> overseers;

    @Autowired
    @Qualifier("analysisTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private CdmMetadataService cdmMetadataService;
    @Autowired
    private CallbackService callbackService;
    @Autowired
    private DriverLocations drivers;
    @Autowired
    private DockerEnvironmentService dockerDescriptorService;
    @Autowired
    private DescriptorServiceImpl tarballDescriptorService;


    @Value("${submission.update.interval}")
    private int submissionUpdateInterval;
    @Autowired
    private List<CredentialsProvider> credentialProviders;

    @Autowired
    public AnalysisService(
            List<ExecutionService> executionServices,
            @Value("${runtime.timeOutSec:259200}") long runtimeTimeout
    ) {
        this.executionServices = executionServices.stream().collect(
                Collectors.toMap(ExecutionService::getExtension, Function.identity())
        );
        // Double the runtime timeout to ensure entries stay in map for some time after timeout completion,
        // so that status information is still available
        overseers = CacheBuilder.newBuilder().expireAfterWrite(
                2 * runtimeTimeout, TimeUnit.SECONDS
        ).<Long, Overseer>build().asMap();
    }

    public Optional<Overseer> analyze(
            AnalysisSyncRequestDTO analysis, File analysisDir, Boolean attachCdmMetadata, BiConsumer<String, String> callback, Integer updateInterval
    ) {
        Validate.notNull(analysis, "analysis can't be null");

        String executableFileName = analysis.getExecutableFileName();
        String fileExtension = Files.getFileExtension(executableFileName).toLowerCase();

        analysis.setResultExclusions(Stream.of(analysis.getResultExclusions(), drivers.getPathExclusions())
                .filter(StringUtils::isNotBlank).collect(Collectors.joining(",")));

        return Optional.ofNullable(executionServices.get(fileExtension)).map(executionService -> {
            try {
                ContainerSupport.patchUrl(analysis.getDataSource());
            } catch (ExecutionInitException e) {
                return new FailedOverseer(Instant.now(), e.getMessage(), AnalysisRequestTypeDTO.R, e.getCause());
            }
            DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
            Path path = analysisDir.toPath();
            AuthEffects auth = credentialProviders.stream().map(provider ->
                    provider.apply(dataSource, path, path.toAbsolutePath().toString())
            ).filter(Objects::nonNull).findFirst().orElse(null);

            if (auth instanceof AuthEffects.ModifyUrl) {
                String newUrl = ((AuthEffects.ModifyUrl) auth).getNewUrl();
                dataSource.setConnectionString(newUrl);
            }
            return executionService.analyze(analysis, analysisDir, callback, updateInterval, auth).whenComplete((outcome, throwable) -> {
                if (auth instanceof AuthEffects.Cleanup) {
                    ((AuthEffects.Cleanup) auth).cleanup();
                }
                if (attachCdmMetadata) {
                    attachMetadata(analysis, analysisDir);
                }
            });
        }).map(overseer -> {
            overseers.put(analysis.getId(), overseer);
            return overseer;
        });
    }

    public Optional<AnalysisResultDTO> abort(Long id) {
        return Optional.ofNullable(overseers.get(id)).map(overseer -> {
            ExecutionOutcome outcome = abort(id, overseer);
            return buildResult(id, Date.from(overseer.getStarted()), fromOutcome(outcome));
        });
    }

    private ExecutionOutcome abort(Long id, Overseer overseer) {
        CompletableFuture<ExecutionOutcome> future = overseer.abort();
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("Execution [{}] waiting for abort operation was interrupted", id);
            return new ExecutionOutcome(Stage.ABORT, "Waiting for abort interrupted", overseer.getStdout());
        } catch (ExecutionException e) {
            log.info("Execution [{}] abort attempt failed", id, e);
            return new ExecutionOutcome(Stage.ABORT, e.getMessage(), overseer.getStdout() + "\r\n" + getStackTrace(e.getCause()));
        } catch (TimeoutException e) {
            log.info("Execution [{}] waiting for abort timed out", id);
            return new ExecutionOutcome(Stage.ABORT, e.getMessage(), overseer.getStdout() + "\r\n" + getStackTrace(e));
        }
    }

    private void attachMetadata(AnalysisSyncRequestDTO analysis, File analysisDir) {
        DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
        try {
            cdmMetadataService.extractMetadata(dataSource, analysisDir);
        } catch (Exception e) {
            log.info("Execution [{}] error collecting CDM metadata for datasource [{}]", analysis.getId(), dataSource.getName(), e);
        }
    }

    public EngineStatus getStatus(List<Long> idsMaybe) {
        Map<Long, ExecutionOutcome> statuses = Optional.ofNullable(idsMaybe).map(ids ->
                ids.stream().flatMap(id ->
                        Optional.ofNullable(overseers.get(id)).map(overseer ->
                                Stream.of(Pair.of(id, getStatus(overseer)))
                        ).orElseGet(Stream::of)
                ).collect(Collectors.toMap(Pair::getKey, Pair::getValue))
        ).orElseGet(Collections::emptyMap);

        List<TarballEnvironmentDTO> tarballs = tarballDescriptorService.getDescriptors().stream().map(DescriptorConverter::toDto).collect(Collectors.toList());
        List<DockerEnvironmentDTO> dockers = dockerDescriptorService.getEnvironments();

        return new EngineStatus(Instant.now(), statuses, new Environments(tarballs, dockers));
    }

    private static ExecutionOutcome getStatus(Overseer overseer) {
        return Optional.ofNullable(overseer.getResult().getNow(null)).orElseGet(() ->
                new ExecutionOutcome(Stage.EXECUTE, null, overseer.getStdout())
        );
    }

    @FileDescriptorCount
    public AnalysisRequestStatusDTO analyze(
            AnalysisRequestDTO analysis, File analysisDir, Boolean compressedResult, Boolean attachCdmMetadata, Long chunkSize
    ) {
        String password = analysis.getCallbackPassword();
        BiConsumer<String, String> callback = (stage, log) -> callbackService.updateAnalysisStatus(analysis, stage, log);
        try {
            return analyze(analysis, analysisDir, attachCdmMetadata, callback, submissionUpdateInterval).map(overseer -> {
                overseer.whenComplete((outcome, throwable) -> {
                    log.info("Execution [{}] completed, sending results...", analysis.getId());
                    AnalysisResultDTO result = buildResult(analysis, outcome, throwable);
                    String url = analysis.getResultCallback();
                    try (AutoCloseWrapper<List<FileSystemResource>> results = callbackService.packResults(analysis, analysisDir, compressedResult, chunkSize)) {
                        callbackService.sendResults(result, results.getValue(), url, password);
                    } catch (ZipException | RuntimeException exception) {
                        result.setError(outcome.addError("Error processing result files: " + exception.getMessage()).getError());
                        callbackService.sendResults(result, null, url, password);
                    }
                });
                return new AnalysisRequestStatusDTO(analysis.getId(), overseer.getType(), overseer.getEnvironment());
            }).orElseGet(() -> {
                log.info("Execution [{}] runtime file type is not recognized.", analysis.getId());
                return new AnalysisRequestStatusDTO(analysis.getId(), NOT_RECOGNIZED, null);
            });
        } catch (Throwable e) {
            log.error("Execution [{}] init failed", analysis.getId(), e);
            return new AnalysisRequestStatusDTO(analysis.getId(), NOT_RECOGNIZED, null);
        }
    }

    public AnalysisResultDTO buildResult(AnalysisSyncRequestDTO analysis, ExecutionOutcome outcome, Throwable throwable) {
        Consumer<AnalysisResultDTO> strategy = throwable != null ? fromError(throwable) : fromOutcome(outcome);
        return buildResult(analysis.getId(), analysis.getRequested(), strategy);
    }

    public AnalysisResultDTO buildResult(Long id, Date requested, Consumer<AnalysisResultDTO> strategy) {
        AnalysisResultDTO result = new AnalysisResultDTO();
        result.setId(id);
        result.setRequested(requested);
        strategy.accept(result);
        result.setStatus((Stage.COMPLETED.equals(result.getStage()) && result.getError() == null) ? AnalysisResultStatusDTO.EXECUTED : AnalysisResultStatusDTO.FAILED);
        return result;
    }

    private Consumer<AnalysisResultDTO> fromOutcome(ExecutionOutcome outcome) {
        return result -> {
            result.setStage(outcome.getStage());
            result.setError(outcome.getError());
            result.setStdout(outcome.getStdout());
        };
    }

    private Consumer<AnalysisResultDTO> fromError(Throwable throwable) {
        return result -> {
            result.setStage(Stage.INITIALIZE);
            result.setError(throwable.getMessage());
            result.setStdout(getStackTrace(throwable));
        };
    }

    public int activeTasks() {

        return threadPoolTaskExecutor.getActiveCount();
    }

}

package com.odysseusinc.arachne.executionengine.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.executionengine.config.properties.DockerProperties;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.service.DockerService;
import com.odysseusinc.arachne.executionengine.util.AnalysisCallback;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "use.docker", havingValue = "true")
public class DockerServiceImpl implements DockerService
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerServiceImpl.class);
    private final ExecutorService executor;
    private final DockerProperties dockerProperties;

    @Autowired
    public DockerServiceImpl(ThreadPoolTaskExecutor taskExecutor, DockerProperties dockerProperties) {
        this.executor = Executors.newSingleThreadExecutor();
        this.dockerProperties = dockerProperties;
    }

    public DockerClient dockerClient() {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerTlsVerify(false);
        Optional.ofNullable(dockerProperties.getRegistryUrl()).ifPresent(builder::withRegistryUrl);
        Optional.ofNullable(dockerProperties.getRegistryPassword()).ifPresent(builder::withRegistryPassword);
        Optional.ofNullable(dockerProperties.getRegistryUsername()).ifPresent(builder::withRegistryUsername);
        DefaultDockerClientConfig config = builder.build();
        DockerHttpClient dockerHttpClient;
        String dockerSocketPath = System.getProperty("use.socket.path");

        if (dockerSocketPath != null) {
            dockerHttpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(URI.create("unix://" + dockerSocketPath))
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(50)
                    .build();
        } else {
            dockerHttpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost()) // Use the default Docker host
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(50)
                    .build();
        }

        return DockerClientImpl.getInstance(config, dockerHttpClient);
    }

    private List<String> listImages(DockerClient client) {
        List<Image> images = client.listImagesCmd().exec();
        if (images == null) {
            return Collections.emptyList();
        }
        return images.stream().map(Image::getRepoTags).filter(Objects::nonNull).flatMap(Arrays::stream).collect(Collectors.toList());
    }

    private String createContainer(DockerClient dockerClient, AnalysisSyncRequestDTO analysis, String imageName) {
        LOGGER.info("Creating container for image: {} and analysis Id: {}", imageName, analysis.getId());
        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withStdinOpen(true)
                .withTty(true)
                .exec();
        LOGGER.info("Container created for image {} with container id {}", imageName, container.getId());
        return container.getId();
    }

    private void runContainer(DockerClient dockerClient, String containerId, File analysisDir, AnalysisSyncRequestDTO analysis, AnalysisCallback callback) {
        String execFileName = analysis.getExecutableFileName();

        LOGGER.info("Starting container: {}", containerId);
        dockerClient.startContainerCmd(containerId).exec();

        LOGGER.info("Copying study into container: {}", containerId);
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(analysisDir.getPath())
                .withRemotePath("/code").exec();

        String baseDir = "/code/" + analysisDir.getName() + "/";
        String workingDir = baseDir + (execFileName.split("/").length > 1 ? execFileName.split("/")[0] : "");
        LOGGER.info("Working dir {}", workingDir);
        String rscriptPath = baseDir + execFileName;
        LOGGER.info("Script path {}", rscriptPath);
        String runCommandId = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("Rscript", rscriptPath)
                .withWorkingDir(workingDir).exec()
                .getId();

        ResultCallback.Adapter<Frame> adapter = getAdapter(analysisDir, analysis, callback);
        try {
            LOGGER.info("Running script: {}", rscriptPath);
            dockerClient.execStartCmd(runCommandId).exec(adapter).awaitCompletion();
        } catch (InterruptedException e) {
            LOGGER.error("InterruptedException occurred whilst running {} in {}, message: {}", rscriptPath, containerId, e.getMessage());
            callback.execute(AnalysisResultStatusDTO.FAILED, null, analysisDir, e);
        } finally {
            stop(dockerClient, containerId);
        }
    }

    private ResultCallback.Adapter<Frame> getAdapter(File analysisDir, AnalysisSyncRequestDTO analysis, AnalysisCallback callback) {
        return new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame item) {
                LOGGER.info(item.toString(), analysis);
                try {
                    String output = new String(item.getPayload());
                    LOGGER.info("Execution output: {}", output);
                    callback.execute(AnalysisResultStatusDTO.EXECUTED, output, analysisDir, null);
                } catch (Exception e) {
                    LOGGER.error("Error processing execution output", e);
                    callback.execute(AnalysisResultStatusDTO.FAILED, null, analysisDir, e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("Error during execution", throwable);
                try {
                    String error = throwable.getMessage();
                    LOGGER.error("Execution error: {}", error);
                    callback.execute(AnalysisResultStatusDTO.FAILED, error, analysisDir, null);
                } catch (Exception e) {
                    LOGGER.error("Error processing execution error", e);
                    callback.execute(AnalysisResultStatusDTO.FAILED, null, analysisDir, e);
                }
            }

            @Override
            public void onComplete() {
                LOGGER.info("Execution completed");
            }

            @Override
            public void close() throws IOException {
                super.close();
            }
        };
    }

    private void stop(DockerClient dockerClient, String containerId) {
        LOGGER.info("Stopping container: {}", containerId);
        dockerClient.stopContainerCmd(containerId).exec();
    }

    private void remove(DockerClient dockerClient, String containerId) {
        dockerClient.removeContainerCmd(containerId)
                .withRemoveVolumes(true)
                .exec();
    }

    @Override
    public Future<?> analyze(AnalysisSyncRequestDTO analysis, File file, DescriptorBundle descriptorBundle, StdoutHandlerParams stdoutHandlerParams, AnalysisCallback callback, KrbConfig krbConfig) {
        return executor.submit(() -> {
            try (DockerClient dockerClient = dockerClient()) {
                LOGGER.info("Execution engine is creating a Docker container from image: " + descriptorBundle.getDescriptor().getBundleName(), analysis);
                String containerId = createContainer(dockerClient, analysis, descriptorBundle.getDescriptor().getBundleName());
                runContainer(dockerClient, containerId, file, analysis, callback);
                this.remove(dockerClient, containerId);
            } catch (Exception e) {
                LOGGER.error("Analysis with id={} failed to execute in Docker", analysis.getId(), e);
                callback.execute(AnalysisResultStatusDTO.FAILED, null, file, e);
            }
        });
    }
}

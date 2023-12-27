package com.odysseusinc.arachne.executionengine.execution.r;

import static com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO.R;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.*;
import com.odysseusinc.arachne.executionengine.config.properties.DockerProperties;
import com.odysseusinc.arachne.executionengine.execution.AbstractOverseer;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DockerService extends RService {
    @Autowired
    @Qualifier("analysisTaskExecutor")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private DockerProperties dockerProperties;

    @Override
    public Overseer analyze(
            AnalysisSyncRequestDTO analysis, File file, DescriptorBundle bundle,
            KrbConfig krbConfig, BiConsumer<String, String> callback, Integer updateInterval) {
        Instant started = Instant.now();
        StringBuffer stdout = new StringBuffer();
        DataSourceUnsecuredDTO dataSource = analysis.getDataSource();
        BiConsumer<String, String> callbackWithStdOut = callback.andThen((stage, out) -> {
            stdout.append(out);
        });

        CompletableFuture<ExecutionOutcome> future = CompletableFuture.supplyAsync(() -> {
            try (DockerClient client = dockerClient()) {
                String image = analysis.getDockerImage();
                buildRuntimeEnvVariables(dataSource, krbConfig.getIsolatedRuntimeEnvs());
                pullImage(image);
                log.info("Creating container for image: {} and analysis Id: {}", image, analysis.getId());
                CreateContainerResponse container = client.createContainerCmd(image)
                        .withStdinOpen(true)
                        .withTty(true)
                        .exec();
                log.info("Container created for image {} with container id {}", image, container.getId());
                String containerId = container.getId();
                runContainer(client, containerId, file, analysis, callbackWithStdOut);
                return new ExecutionOutcome(Stage.COMPLETED, null, "");
            } catch (IOException e) {
                log.error("Execution [{}] failed:", analysis.getId(), e);
                callbackWithStdOut.accept(Stage.EXECUTE, e.getMessage());
                return new ExecutionOutcome(Stage.COMPLETED, e.getMessage(), "");
            }
        }, executor);
        return new DockerService.DockerOverseer(analysis.getId(), started, stdout, future);
    }

    private void runContainer(DockerClient dockerClient, String containerId, File analysisDir, AnalysisSyncRequestDTO analysis, BiConsumer<String, String> callback) {
        String execFileName = analysis.getExecutableFileName();

        log.info("Starting container: {}", containerId);
        dockerClient.startContainerCmd(containerId).exec();

        String codeDir = "/bin";
        log.info("Copying study into container: {}", containerId);
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(analysisDir.getPath())
                .withRemotePath(codeDir).exec();

        String baseDir = codeDir + "/" + analysisDir.getName() + "/";
        String workingDir = baseDir + (execFileName.split("/").length > 1 ? execFileName.split("/")[0] : "");
        log.info("Working dir {}", workingDir);
        String rscriptPath = baseDir + execFileName;
        log.info("Script path {}", rscriptPath);
        String runCommandId = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("Rscript", rscriptPath)
                .withWorkingDir(workingDir).exec()
                .getId();

        ResultCallback.Adapter<Frame> adapter = getAdapter(analysis, callback);
        try {
            log.info("Running script: {}", rscriptPath);
            dockerClient.execStartCmd(runCommandId).exec(adapter).awaitCompletion(runtimeTimeOutSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred whilst running {} in {}, message: {}", rscriptPath, containerId, e.getMessage());
            callback.accept(Stage.EXECUTE, e.getMessage());
        } finally {
            stop(dockerClient, containerId);
            remove(dockerClient, containerId);
        }
    }

    private ResultCallback.Adapter<Frame> getAdapter(AnalysisSyncRequestDTO analysis, BiConsumer<String, String> callback) {
        return new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame item) {
                log.info(item.toString(), analysis);
                try {
                    String output = new String(item.getPayload(), StandardCharsets.UTF_8);
                    if (item.getStreamType() == StreamType.STDERR) {
                        output = "ERROR: " + output;
                    }
                    log.info("Execution output: {}", output);
                    callback.accept(Stage.EXECUTE, output);
                } catch (RuntimeException e) {
                    log.error("Error processing execution output", e);
                    callback.accept(Stage.EXECUTE, e.getMessage());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                String error = throwable.getMessage();
                log.error("Execution error: {}", error);
                callback.accept(Stage.ABORTED, error);
            }

            @Override
            public void onComplete() {
                log.info("Execution completed");
            }

            @Override
            public void close() throws IOException {
                super.close();
            }
        };
    }

    private void stop(DockerClient dockerClient, String containerId) {
        log.info("Stopping container: {}", containerId);
        dockerClient.stopContainerCmd(containerId).exec();
    }

    private void remove(DockerClient dockerClient, String containerId) {
        dockerClient.removeContainerCmd(containerId)
                .withRemoveVolumes(true)
                .exec();
    }

    public void pullImage(String imageName) {
        log.info("Downloading " + imageName + " ... this may take some time, but we only need to do it once");
        PullImageResultCallback callback = new PullImageResultCallback() {
            @Override
            public void onError(Throwable throwable) {
                log.error("Failed to pull image" + throwable.getMessage());
                super.onError(throwable);
            }
        };
        try (DockerClient client = dockerClient()) {
            client.pullImageCmd(imageName).exec(callback).awaitCompletion();
        } catch (IOException | InterruptedException ex) {
            log.error(ex.toString());
            throw new RuntimeException("Failed to pull image" + ex.getMessage(), ex);
        }
    }

    public DockerClient dockerClient() {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerTlsVerify(false);
        Optional.ofNullable(dockerProperties.getRegistryUrl()).ifPresent(builder::withRegistryUrl);
        Optional.ofNullable(dockerProperties.getRegistryPassword()).ifPresent(builder::withRegistryPassword);
        Optional.ofNullable(dockerProperties.getRegistryUsername()).ifPresent(builder::withRegistryUsername);
        DefaultDockerClientConfig config = builder.build();

        URI host = Optional.ofNullable(System.getProperty("docker.socket")).map(path ->
                URI.create("unix://" + path)
        ).orElseGet(config::getDockerHost);
        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(host)
                .sslConfig(config.getSSLConfig())
                .maxConnections(50)
                .build();

        return DockerClientImpl.getInstance(config, dockerHttpClient);
    }

    @Getter
    public static class DockerOverseer extends AbstractOverseer {
        private final StringBuffer stdout;

        public DockerOverseer(long id, Instant started, StringBuffer stdout, CompletableFuture<ExecutionOutcome> result) {
            super(id, (stage, out) -> {}, started, null, 0, result);
            this.stdout = stdout;
        }

        @Override
        public String getStdout() {
            return stdout.toString();
        }

        @Override
        public CompletableFuture<ExecutionOutcome> abort() {
            return result.isDone() ? result : CompletableFuture.completedFuture(
                    new ExecutionOutcome(Stage.ABORT, "Abort is not supported for R analysis", null)
            );

        }

        @Override
        public AnalysisRequestTypeDTO getType() {
            return R;
        }

    }

    protected Map<String, String> buildRuntimeEnvVariables(DataSourceUnsecuredDTO dataSource, Map<String, String> krbProps) {
        return super.buildRuntimeEnvVariables(dataSource, krbProps);
    }
}

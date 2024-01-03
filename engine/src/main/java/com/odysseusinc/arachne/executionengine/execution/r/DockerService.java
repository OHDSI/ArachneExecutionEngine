package com.odysseusinc.arachne.executionengine.execution.r;

import static com.github.dockerjava.api.model.ResponseItem.ProgressDetail;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import com.odysseusinc.arachne.executionengine.config.properties.DockerRegistryProperties;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.datasourcemanager.krblogin.KrbConfig;
import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public class DockerService extends RService implements AutoCloseable {
    private static final String WORKDIR = "/etc/analysis";
    private final DockerClient client;
    /**
     * Directory prefix on HOST where the analysis files are unpacked.
     */
    @Value("${analysis.mount}")
    private String analysisMount;
    /**
     * Socket to connect
     */
    @Value("${docker.host:#{null}}")
    private String host;

    @Value("docker.certPath:#{null}")
    private String certPath;

    @Value("${docker.image.default}")
    private String defaultImage;

    @Autowired
    @Qualifier("analysisTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    public DockerService(DockerRegistryProperties properties) {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerTlsVerify(certPath != null)
                .withDockerCertPath(certPath)
                .withRegistryUrl(properties.getUrl())
                .withRegistryUsername(properties.getUsername())
                .withRegistryPassword(properties.getPasword());
        Optional.ofNullable(host).ifPresent(builder::withDockerHost);
        DefaultDockerClientConfig config = builder.build();

        URI host = config.getDockerHost();
        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(host)
                .sslConfig(config.getSSLConfig())
                .maxConnections(50)
                .build();
        client = DockerClientImpl.getInstance(config, dockerHttpClient);
        log.info("Initialized Docker interface [{}]", host);
    }

    @Override
    protected Overseer analyze(
            AnalysisSyncRequestDTO analysis, File analysisDir, BiConsumer<String, String> sendCallback, Integer updateInterval, KrbConfig krbConfig
    ) {
        List<String> env = buildRuntimeEnvVariables(analysis.getDataSource(), krbConfig.getIsolatedRuntimeEnvs()).entrySet().stream().map(entry ->
                entry.getKey() + "=" + entry.getValue()).collect(Collectors.toList()
        );
        Instant started = Instant.now();
        Long id = analysis.getId();
        String image = Optional.ofNullable(analysis.getDockerImage()).orElse(defaultImage);

        StringBuffer stdout = new StringBuffer();
        String script = WORKDIR + "/" + analysis.getExecutableFileName();
        BiConsumer<String, String> callback = (stage, out) -> {
            stdout.append(out);
            sendCallback.accept(stage, out);
        };

        CompletableFuture<String> init = CompletableFuture.supplyAsync(
                () -> {
                    log.info("Execution [{}] use Docker image [{}]", id, image);
                    pullImage(callback, id, image, stdout, client);
                    callback.accept(Stage.INITIALIZE, "Pull complete, creating container\r\n");
                    log.info("Execution [{}] creating container with image [{}]", id, image);
                    String containerId = createContainer(analysisDir, env, image, script);
                    log.info("Execution [{}] created container [{}]", id, containerId);
                    callback.accept(Stage.INITIALIZE, "Container [" + containerId + "] created with [" + analysisDir.getPath() + "] mounted for execution\r\n");
                    client.startContainerCmd(containerId).exec();
                    return containerId;
                },
                executor
        );

        return new DockerOverseer(id, client, started, runtimeTimeOutSec, stdout, init, updateInterval, sendCallback, image, killTimeoutSec);
    }

    private void pullImage(BiConsumer<String, String> callback, Long id, String image, StringBuffer stdout, DockerClient client) {
        try {
            callback.accept(Stage.INITIALIZE, "Pulling image [" + image + "]\r\n");
            client.pullImageCmd(image).exec(new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    super.onNext(item);
                    ProgressDetail progress = item.getProgressDetail();
                    if (progress != null) {
                        log.info("Downloading [{}]: {} / {}", image, progress.getCurrent(), progress.getTotal());
                        stdout.append(" -> ").append(progress.getCurrent());
                    }
                }

                @Override
                public void onComplete() {
                    super.onComplete();
                    log.info("Completed pulling [{}]", image);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("Failed to pull image [{}]: {}", image, throwable.getMessage());
                    super.onError(throwable);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            log.error("Execution [{}] failed:", id, e);
            callback.accept(Stage.INITIALIZE, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Execution [" + id + "] failed to initialize", e);
        }
    }

    private String createContainer(File analysisDir, List<String> env, String image, String script) {
        String hostPath = analysisMount + "/" + analysisDir.getName();
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(hostPath, new Volume(WORKDIR)))
                .withAutoRemove(true);
        CreateContainerResponse container = client.createContainerCmd(image)
                .withHostConfig(hostConfig)
                .withEnv(env)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(RService.EXECUTION_COMMAND, script)
                .withWorkingDir(WORKDIR)
                .exec();
        return container.getId();
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}

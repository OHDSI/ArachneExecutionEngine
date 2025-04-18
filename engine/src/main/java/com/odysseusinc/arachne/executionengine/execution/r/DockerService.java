package com.odysseusinc.arachne.executionengine.execution.r;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.DockerEnvironmentDTO;
import com.odysseusinc.arachne.executionengine.config.properties.DockerRegistryProperties;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.util.Streams;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.dockerjava.api.model.ResponseItem.ProgressDetail;
import static com.odysseusinc.arachne.executionengine.execution.r.DockerPullPolicy.ALWAYS;
import static com.odysseusinc.arachne.executionengine.execution.r.DockerPullPolicy.FORCE;
import static com.odysseusinc.arachne.executionengine.execution.r.DockerPullPolicy.NEVER;

@Service
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

    @Getter
    @Value("${docker.image.default:#{null}}")
    private String defaultImage;

    @Getter
    @Value("${docker.image.filter:#{null}}")
    private String filterRegex;

    @Value("${docker.image.pull:#{T(com.odysseusinc.arachne.executionengine.execution.r.DockerPullPolicy).MISSING}}")
    private DockerPullPolicy pull;

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
    }

    @PostConstruct
    public void init() {
        log.info("Initialized Docker interface [{}], pull policy: {}", host, pull);
    }

    @Override
    protected Overseer analyze(
            AnalysisSyncRequestDTO analysis, File analysisDir, Integer updateInterval, Map<String, String> envp, BiConsumer<String, String> sendCallback
    ) {
        List<String> env = envp.entrySet().stream().map(entry ->
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
                    log.info("Execution [{}] use Docker image [{}], pull policy: {}", id, image, pull);
                    if (pull == FORCE || pull == ALWAYS) {
                        log.info("Execution [{}] force pull image image {}", id, image);
                        callback.accept(Stage.INITIALIZE, "Force pull image [" + image + "]\r\n");
                        try {
                            pullImage(callback, id, image, stdout, client);
                            callback.accept(Stage.INITIALIZE, "Pull complete, creating container\r\n");
                        } catch (DockerException e) {
                            if (pull == ALWAYS && imageExists(image)) {
                                log.warn("Execution [{}] pull failed: {}, execution will proceed with image found locally", id, e.getMessage());
                                callback.accept(Stage.INITIALIZE, "Execution [" + id + "] pull failed: " + e.getMessage() + ", proceed with local image");
                            } else {
                                log.error("Execution [{}] pull failed: ", id, e);
                                callback.accept(Stage.INITIALIZE, ExceptionUtils.getStackTrace(e));
                                throw e;
                            }
                        }
                    } else if (imageExists(image)) {
                        log.info("Execution [{}] image found, will proceed without pull", id);
                        callback.accept(Stage.INITIALIZE, "Image found, proceed to execution\r\n");
                    } else if (pull == NEVER) {
                        log.info("Execution [{}] image not found, aborting", id);
                        callback.accept(Stage.INITIALIZE, "Image image not found, aborting\r\n");
                        throw new NotFoundException("Image [" + image + "] not found");
                    } else {
                        log.info("Execution [{}] image not found, proceed with pull", id);
                        callback.accept(Stage.INITIALIZE, "Image [" + image + "] not found, proceed to pull\r\n");
                        pullImage(callback, id, image, stdout, client);
                        callback.accept(Stage.INITIALIZE, "Pull complete, creating container\r\n");
                    }
                    log.info("Execution [{}] creating container with image [{}]", id, image);
                    String containerId = createContainer(analysisDir, env, image, script);
                    log.info("Execution [{}] created container [{}]", id, containerId);
                    callback.accept(Stage.INITIALIZE, "Container [" + containerId + "] created with [" + analysisDir.getPath() + "] mounted for execution\r\n");
                    client.startContainerCmd(containerId).exec();
                    return containerId;
                },
                executor
        );
        // Look for exact tag here, ignore configuration!
        // We will only truly know the image after the execution, but need to return something right away.
        String imageTagOrId = listEnvironments(image).findFirst().map(DockerEnvironmentDTO::getImageId).orElse(image);
        return new DockerOverseer(id, client, started, runtimeTimeOutSec, stdout, init, updateInterval, sendCallback, imageTagOrId, killTimeoutSec);
    }

    private boolean imageExists(String image) {
        try {
            return client.inspectImageCmd(image).exec() != null;
        } catch (NotFoundException e) {
            return false;
        }
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

    public List<DockerEnvironmentDTO> listEnvironments() {
        return Optional.ofNullable(filterRegex).map(regex ->
                Stream.concat(
                        Streams.from(defaultImage()), listEnvironments(regex)
                ).collect(Collectors.toList())
        ).orElseGet(
                () -> defaultImage().map(Collections::singletonList).orElse(null)
        );
    }

    private Optional<DockerEnvironmentDTO> defaultImage() {
        return Optional.ofNullable(defaultImage).map(img -> new DockerEnvironmentDTO(null, Arrays.asList(defaultImage)));
    }

    private Stream<DockerEnvironmentDTO> listEnvironments(String filterRegex) {
        List<Image> images = client.listImagesCmd().exec();
        return images.stream().flatMap(image -> {
            List<String> tags = getTags(image, filterRegex);
            return tags.isEmpty() ? Stream.empty() : Stream.of(new DockerEnvironmentDTO(image.getId(), tags));
        });
    }

    private List<String> getTags(Image image, String imageFilterRegex) {
        return Streams.ofNullable(image.getRepoTags()).flatMap(Arrays::stream).filter(tag ->
                tag.matches(imageFilterRegex)
        ).collect(Collectors.toList());
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}

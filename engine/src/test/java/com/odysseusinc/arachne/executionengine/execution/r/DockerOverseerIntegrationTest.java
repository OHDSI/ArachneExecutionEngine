package com.odysseusinc.arachne.executionengine.execution.r;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.ExecutionOutcome;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.Stage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration test for {@link DockerOverseer}. Requires a running Docker daemon; the whole class is
 * skipped (assumption aborted) when Docker is not reachable. Each test starts a real short-lived
 * container running actual R code and asserts the resulting {@link ExecutionOutcome} status.
 */
class DockerOverseerIntegrationTest {

    private static final String IMAGE = "r-base:4.4.1";
    private static final int TIMEOUT_SEC = 120;
    private static final int UPDATE_INTERVAL_MS = 500;
    private static final int KILL_TIMEOUT_SEC = 10;

    private static DockerClient client;
    private final List<String> startedContainers = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        boolean available;
        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(10)
                    .build();
            client = DockerClientImpl.getInstance(config, httpClient);
            client.pingCmd().exec();
            available = true;
        } catch (Throwable t) {
            available = false;
        }
        Assumptions.assumeTrue(available, "Docker daemon is not available, skipping DockerOverseer integration test");
        ensureImage();
    }

    private static void ensureImage() {
        try {
            client.inspectImageCmd(IMAGE).exec();
        } catch (NotFoundException notPresent) {
            try {
                client.pullImageCmd(IMAGE).exec(new PullImageResultCallback()).awaitCompletion(3, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assumptions.assumeTrue(false, "Interrupted while pulling " + IMAGE);
            }
        }
    }

    @AfterEach
    void cleanUp() {
        for (String id : startedContainers) {
            try {
                client.removeContainerCmd(id).withForce(true).exec();
            } catch (RuntimeException alreadyGone) {
                // container used --rm and was auto-removed on exit, or never created
            }
        }
        startedContainers.clear();
    }

    @Test
    void completedOnSuccess() throws Exception {
        ExecutionOutcome outcome = runR("x <- 1:10; cat('sum:', sum(x), '\\n')").getResult().get(60, TimeUnit.SECONDS);

        assertEquals(Stage.COMPLETED, outcome.getStage());
        assertNull(outcome.getError());
        assertTrue(outcome.getStdout().contains("sum: 55"), "stdout should contain R computation result");
    }

    @Test
    void executeStageOnNonZeroExit() throws Exception {
        ExecutionOutcome outcome = runR("quit(status=3, save='no')").getResult().get(60, TimeUnit.SECONDS);

        assertEquals(Stage.EXECUTE, outcome.getStage());
        assertEquals("Exit code 3", outcome.getError());
    }

    @Test
    void abortedOnCancel() throws Exception {
        DockerOverseer overseer = runR("Sys.sleep(120)");

        ExecutionOutcome ack = overseer.abort().get(5, TimeUnit.SECONDS);
        assertEquals(Stage.ABORT, ack.getStage(), "abort() should acknowledge immediately with ABORT");

        ExecutionOutcome outcome = overseer.getResult().get(30, TimeUnit.SECONDS);
        assertEquals(Stage.ABORTED, outcome.getStage());
    }

    private DockerOverseer runR(String rExpr) {
        CreateContainerResponse container = client.createContainerCmd(IMAGE)
                .withHostConfig(HostConfig.newHostConfig().withAutoRemove(true))
                .withCmd("Rscript", "-e", rExpr)
                .exec();
        startedContainers.add(container.getId());
        client.startContainerCmd(container.getId()).exec();
        return new DockerOverseer(
                1L, client, Instant.now(), TIMEOUT_SEC, new StringBuffer(),
                CompletableFuture.completedFuture(container.getId()), UPDATE_INTERVAL_MS,
                (stage, log) -> { }, IMAGE, KILL_TIMEOUT_SEC
        );
    }
}

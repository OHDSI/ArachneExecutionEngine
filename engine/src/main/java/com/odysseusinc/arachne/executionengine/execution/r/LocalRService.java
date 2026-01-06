package com.odysseusinc.arachne.executionengine.execution.r;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
@Slf4j
@ConditionalOnProperty(name = "runtime.local")
public class LocalRService extends RService {

    @Override
    protected Overseer analyze(AnalysisSyncRequestDTO analysis, File file, Integer updateInterval, Map<String, String> envp, BiConsumer<String, String> callback) {
        Long id = analysis.getId();
        String executableFileName = analysis.getExecutableFileName();

        try {
            Instant started = Instant.now();
            log.info("Execution [{}] using local R environment (runtime.local=true)", id);
            ProcessBuilder pb = new ProcessBuilder(EXECUTION_COMMAND, executableFileName).directory(file).redirectErrorStream(true);
            pb.environment().putAll(envp);
            log.info("Execution [{}] start local R process: {}", id, String.join(" ", new String[]{EXECUTION_COMMAND, executableFileName}));
            Process process = pb.start();

            return new TarballROverseer(
                    id, process, runtimeTimeOutSec, callback, updateInterval, started, "local", killTimeoutSec
            );

        } catch (IOException ex) {
            log.error("Execution [{}] error building runtime command", id, ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}

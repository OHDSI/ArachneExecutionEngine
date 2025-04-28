package com.odysseusinc.arachne.executionengine.execution.r;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisSyncRequestDTO;
import com.odysseusinc.arachne.executionengine.auth.AuthEffects;
import com.odysseusinc.arachne.executionengine.execution.ExecutionService;
import com.odysseusinc.arachne.executionengine.execution.Overseer;
import com.odysseusinc.arachne.executionengine.service.DescriptorService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class RExecutionService implements ExecutionService {
    @Autowired
    private TarballRService tarballService;
    @Autowired
    private DescriptorService descriptorService;
    @Autowired
    private DockerService dockerService;
    @Autowired(required = false)
    private LocalRService localService;

    @Value("${runtime.local:false}")
    private boolean useLocalREnv;

    public String getExtension() {
        return "r";
    }

    @Override
    public Overseer analyze(AnalysisSyncRequestDTO analysis, File dir, BiConsumer<String, String> callback, Integer updateInterval, AuthEffects auth) {
        RService delegate = calcEnv(analysis.getId(), analysis.getDockerImage(), analysis.getRequestedDescriptorId());
        return delegate.analyze(analysis, dir, callback, updateInterval, auth);
    }

    private RService calcEnv(Long id, String image, String descriptorId) {
        if (useLocalREnv) {
            log.info("Analysis [{}] will be executed in LOCAL R environment (forced by runtime.local=true)", id);
            return localService;
        } else if (image != null) {
            log.info("Analysis [{}] requested image [{}], force DOCKER runtime", id, image);
            return dockerService;
        } else if (descriptorId != null) {
            log.info("Analysis [{}] requested descriptor [{}], force TARBALL runtime", id, descriptorId);
            return tarballService;
        } else if (CollectionUtils.isEmpty(descriptorService.getDescriptors())) {
            log.info("Analysis [{}] will be executed in default DOCKER environment", id);
            return dockerService;
        } else {
            log.info("Analysis [{}] will be executed in default TARBALL environment", id);
            return tarballService;
        }
    }

    @PostConstruct
    public void init() {
        if (useLocalREnv) {
            log.info("Runtime service running in LOCAL environment mode");
        }
    }

}
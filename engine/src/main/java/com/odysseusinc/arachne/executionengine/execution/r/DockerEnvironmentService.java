package com.odysseusinc.arachne.executionengine.execution.r;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.DockerEnvironmentDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DockerEnvironmentService {
    @Autowired
    private DockerService service;

    @Getter
    private volatile List<DockerEnvironmentDTO> environments;

    public DockerEnvironmentService() {
        log.info("Instantiated");
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    private void refresh() {
        List<DockerEnvironmentDTO> old = environments;
        try {
            List<DockerEnvironmentDTO> latest = service.listEnvironments();
            // TODO Print diff instead of simplistic size check
            if (old == null || old.size() != latest.size()) {
                log.info("Refreshed DOCKER images ({})", latest.size());
                latest.forEach(desc ->
                        log.info("DOCKER image [{}]: {}", desc.getImageId(), desc.getTags())
                );
            }
            environments = latest;
        } catch (Exception e) {
            log.error("Failed to scan for docker images, regex = [{}], default image = [{}]: {}", service.getFilterRegex(), service.getDefaultImage(), e.getMessage());
            log.debug(e.getMessage(), e);
        }
    }

}

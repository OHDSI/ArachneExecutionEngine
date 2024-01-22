package com.odysseusinc.arachne.executionengine.config.runtimeservice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "runtimeservice.dist")
@Getter
@Setter
public class RIsolatedRuntimeProperties {
    private String[] runCmd;
    private String jailSh;
    private String cleanupSh;
    // Path to default runtime environment
    private String archive;
    private String defaultDescriptorFile;
    // Path to folder with custom runtime environments
    private String archiveFolder = "/runtimes";
    // Flag for showing difference between dependencies
    private boolean applyRuntimeDependenciesComparisonLogic;
}

package com.odysseusinc.arachne.executionengine.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "docker.registry")
public class DockerRegistryProperties {
    private String url;
    private String username;
    private String pasword;
}
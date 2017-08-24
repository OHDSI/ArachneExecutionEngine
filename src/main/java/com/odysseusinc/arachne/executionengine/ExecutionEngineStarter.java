package com.odysseusinc.arachne.executionengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration
@Configuration
@ComponentScan
public class ExecutionEngineStarter {

    public static void main(String[] args) {
        // NOSONAR
        SpringApplication.run(ExecutionEngineStarter.class, args);
    }

}

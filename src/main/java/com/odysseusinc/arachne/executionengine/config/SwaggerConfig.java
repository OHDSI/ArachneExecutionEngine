package com.odysseusinc.arachne.executionengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@ConditionalOnProperty(prefix = "swagger", name = "enable")
public class SwaggerConfig {

    @Value("${swagger.title}")
    private String swaggerTitle;

    @Value("${swagger.description}")
    private String swaggerDescription;

    @Value("${swagger.version}")
    private String swaggerVersion;

    @Value("${swagger.basePackage}")
    private String swaggerBasePackage;

    @Bean
    public Docket api() {

        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage(swaggerBasePackage))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(
                        new ApiInfoBuilder()
                        .title(swaggerTitle)
                        .description(swaggerDescription)
                        .version(swaggerVersion)
                        .build()
                )
                .pathMapping("/")
                .useDefaultResponseMessages(false);
    }

}

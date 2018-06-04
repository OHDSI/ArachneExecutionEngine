/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: March 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.ConnectorServerFactoryBean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.management.MalformedObjectNameException;

@Configuration
@EnableAsync
public class AnalisysConfig {

    @Value("${executor.corePoolSize}")
    private Integer corePoolSize;
    @Value("${executor.maxPoolSize}")
    private Integer maxPoolSize;
    @Value("${executor.queueCapacity}")
    private Integer queueCapacity;

    @Bean(name = "analysisTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }

    @Bean
    public ThreadPoolExecutorMonitor threadPoolExecutorMonitor(){

        return new ThreadPoolExecutorMonitor(taskExecutor());
    }
}

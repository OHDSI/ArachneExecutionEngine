package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvParseStrategy;

import com.odysseusinc.arachne.executionengine.service.impl.DescriptorServiceImpl;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutionRuntimeHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorServiceImpl.class);
    private static Map<String, List<ParseStrategy>> runtimeParseStrategiesMap = new HashMap<>();

    static {
        List<ParseStrategy> rEnvParseStrategies = Arrays.asList(
                new REnvParseStrategy()
        );

        runtimeParseStrategiesMap.put("renv.lock", rEnvParseStrategies);
    }

    public static List<ExecutionRuntime> getRuntimes(List<File> files) {
        return files.stream()
                .filter(file -> runtimeParseStrategiesMap.containsKey(file.getName()))
                .map(file -> {
                    String name = file.getName();
                    List<ParseStrategy> parseStrategies = runtimeParseStrategiesMap.get(name);
                    return parseStrategies.stream()
                            .map(strategyOpt -> {
                                Optional<ExecutionRuntime> maybeStrategy = strategyOpt.getExecutionRuntime(file);
                                String strategy = maybeStrategy.map(runtime -> "[" + runtime.getType() + " version " + runtime.getVersion() + "]").orElse("no runtime");
                                LOGGER.info("From [{}] extracted [{}]", name, strategy);
                                return maybeStrategy.orElse(null);
                            })
                            .findFirst();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}

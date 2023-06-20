package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvParseStrategy;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutionRuntimeHelper {
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
                    List<ParseStrategy> parseStrategies = runtimeParseStrategiesMap.get(file.getName());
                    return parseStrategies.stream()
                            .map(strategyOpt -> strategyOpt.getExecutionRuntime(file))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .findFirst();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}

package com.odysseusinc.arachne.executionengine.model.descriptor.converter;

import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.model.descriptor.converter.r.RExecutionRuntimeConverter;

import java.util.HashMap;
import java.util.Map;

public class ExecutionRuntimeConverterHelper {
    private Map<RuntimeType, ExecutionRuntimeConverter> runtimeConvertStrategiesMap = new HashMap<>();

    public ExecutionRuntimeConverterHelper() {
        runtimeConvertStrategiesMap.put(RuntimeType.R, new RExecutionRuntimeConverter());
    }

    public ExecutionRuntimeConverter getConverter(RuntimeType runtimeType) {
        return runtimeConvertStrategiesMap.get(runtimeType);
    }
}

package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.ParseStrategy;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class REnvParseStrategy implements ParseStrategy {
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Optional<ExecutionRuntime> getExecutionRuntime(File file) {
        try {
            return Optional.of(RExecutionRuntime.fromREnvLock(getLock(file)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private REnvLock getLock(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return mapper.readValue(is, REnvLock.class);
        }
    }
}

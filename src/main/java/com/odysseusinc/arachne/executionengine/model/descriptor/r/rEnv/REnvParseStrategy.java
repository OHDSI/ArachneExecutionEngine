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
    @Override
    public Optional<ExecutionRuntime> getExecutionRuntime(File file) {
        try {
            return Optional.of(RExecutionRuntime.fromREnvLock(getLock(file)));
        } catch (Exception e) {
            // ignore
        }

        return Optional.empty();
    }

    private REnvLock getLock(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(is, REnvLock.class);
    }
}

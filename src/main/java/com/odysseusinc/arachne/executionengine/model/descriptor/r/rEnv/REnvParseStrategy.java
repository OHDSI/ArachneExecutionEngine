package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.ParseStrategy;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class REnvParseStrategy implements ParseStrategy {
    private static final Logger log = LoggerFactory.getLogger(REnvParseStrategy.class);

    private static final String RENV_LOCK = "renv.lock";
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("OptionalAssignedToNull")
    @Override
    public Optional<ExecutionRuntime> apply(String name, InputStream is) {
        if (name.equals(RENV_LOCK) || name.endsWith(File.separator + RENV_LOCK)) {
            try {
                REnvLock lock = mapper.readValue(is, REnvLock.class);
                return Optional.of(RExecutionRuntime.fromREnvLock(lock));
            } catch (IOException e) {
                log.warn("Error parsing [{}] as renv.lock: {}", name, e.getMessage());
                return Optional.empty();
            }
        } else {
            return null;
        }
    }
}

package com.odysseusinc.arachne.executionengine.model.descriptor;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.BiFunction;

@FunctionalInterface
public interface ParseStrategy extends BiFunction<String, InputStream, Optional<? extends ExecutionRuntime>> {
    /**
     *
     * @param name file name
     * @param is input stream containing the file content
     * @return null if parse strategy doesn't match the filename. This indicates
     * that input stream was not consumed and the next strategy should be tried
     * <br> An empty Optional if strategy matched but did not yield any descriptor.
     * This indicates that input stream was fully or partially consumed.
     * <br> A non-empty Optional containing parsed execution runtime.
     * This also indicates, that input stream was consumed.
     */
    @Override
    Optional<? extends ExecutionRuntime> apply(String name, InputStream is);
}

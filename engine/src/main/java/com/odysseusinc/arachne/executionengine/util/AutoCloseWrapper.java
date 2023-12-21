package com.odysseusinc.arachne.executionengine.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Wraps value with actions to be performed to dispose the resoiurces
 */
@AllArgsConstructor(staticName = "of")
public class AutoCloseWrapper<T> implements AutoCloseable {
    @Getter
    private final T value;
    private final Runnable dispose;

    @Override
    public void close() {
        dispose.run();
    }
}

package com.odysseusinc.arachne.executionengine.util;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class Streams {
    public static <T> Stream<T> from(Optional<T> optional) {
        return optional.map(Stream::of).orElseGet(Stream::of);
    }

    public static <T> Stream<T> safe(Collection<T> collection) {
        return collection == null ? Stream.of() : collection.stream();
    }

    public static <T> Stream<T> ofNullable(T objects) {
        return objects == null ? Stream.of() : Stream.of(objects);
    }
}

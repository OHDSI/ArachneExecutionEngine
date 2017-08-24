package com.odysseusinc.arachne.executionengine.service.sql;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T, R> {

    R apply(T value) throws SQLException;
}

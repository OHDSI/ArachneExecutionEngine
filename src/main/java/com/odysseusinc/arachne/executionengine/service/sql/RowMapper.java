package com.odysseusinc.arachne.executionengine.service.sql;

import java.sql.ResultSet;

@FunctionalInterface
public interface RowMapper<T> extends SqlFunction<ResultSet, T> {

}

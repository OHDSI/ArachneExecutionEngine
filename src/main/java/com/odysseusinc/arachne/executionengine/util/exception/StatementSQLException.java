package com.odysseusinc.arachne.executionengine.util.exception;

import java.sql.SQLException;

public class StatementSQLException extends SQLException {

    private String statement;

    public StatementSQLException(String reason, Throwable cause, String statement) {

        super(reason, cause);
        this.statement = statement;
    }

    public String getStatement() {

        return statement;
    }
}

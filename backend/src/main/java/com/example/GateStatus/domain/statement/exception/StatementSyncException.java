package com.example.GateStatus.domain.statement.exception;

public class StatementSyncException extends RuntimeException {

    public StatementSyncException(String message) {
        super(message);
    }

    public StatementSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.example.GateStatus.domain.statement.exception;

public abstract class StatementException extends RuntimeException {

    public StatementException(String message) {
        super(message);
    }
}

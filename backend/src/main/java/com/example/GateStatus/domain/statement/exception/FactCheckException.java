package com.example.GateStatus.domain.statement.exception;

public class FactCheckException extends RuntimeException {

    public FactCheckException(String message) {
        super(message);
    }

    public FactCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}

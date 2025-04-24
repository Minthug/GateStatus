package com.example.GateStatus.domain.comparison.exception;

public abstract class CompareException extends RuntimeException {
    public CompareException(String message) {
        super(message);
    }
}

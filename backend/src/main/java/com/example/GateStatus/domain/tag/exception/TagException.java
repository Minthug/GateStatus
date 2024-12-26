package com.example.GateStatus.domain.tag.exception;

public abstract class TagException extends RuntimeException {
    public TagException(String message) {
        super(message);
    }
}

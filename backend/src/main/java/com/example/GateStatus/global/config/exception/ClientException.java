package com.example.GateStatus.global.config.exception;

public abstract class ClientException extends RuntimeException {
    public ClientException(String message) {
        super(message);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }
}

package com.example.GateStatus.global.config.exception;

import java.time.LocalDateTime;

public record ErrorResponse(String message, String code, LocalDateTime timestamp) {

    public ErrorResponse(String message) {
        this(message, "ERR_API", LocalDateTime.now());
    }
}

package com.example.GateStatus.domain.timeline.exception;

public abstract class TimelineException extends RuntimeException {
    public TimelineException(String message) {
        super(message);
    }

    public TimelineException(String message, Throwable cause) {
        super(message, cause);
    }
}

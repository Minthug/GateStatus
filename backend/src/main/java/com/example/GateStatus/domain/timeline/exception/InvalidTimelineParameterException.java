package com.example.GateStatus.domain.timeline.exception;

public class InvalidTimelineParameterException extends TimelineException {
    public InvalidTimelineParameterException(String message) {
        super(message);
    }

    public InvalidTimelineParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}

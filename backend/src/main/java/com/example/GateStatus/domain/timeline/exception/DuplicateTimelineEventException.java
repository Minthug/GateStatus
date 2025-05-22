package com.example.GateStatus.domain.timeline.exception;

public class DuplicateTimelineEventException extends TimelineException {
    public DuplicateTimelineEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateTimelineEventException(String message) {
        super(message);
    }
}

package com.example.GateStatus.domain.issue.exception;

public abstract class IssueException extends RuntimeException {
    public IssueException(String message) {
        super(message);
    }
}

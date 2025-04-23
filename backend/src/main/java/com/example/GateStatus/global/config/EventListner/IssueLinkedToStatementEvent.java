package com.example.GateStatus.global.config.EventListner;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class IssueLinkedToStatementEvent implements DomainEvent {

    private final String eventId;
    private final String issueId;
    private final String statementId;
    private final LocalDateTime occurredAt;

    public IssueLinkedToStatementEvent(String issueId, String statementId) {
        this.eventId = UUID.randomUUID().toString();
        this.issueId = issueId;
        this.statementId = statementId;
        this.occurredAt = LocalDateTime.now();
    }
}

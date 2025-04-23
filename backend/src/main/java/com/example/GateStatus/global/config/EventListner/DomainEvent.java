package com.example.GateStatus.global.config.EventListner;

import java.time.LocalDateTime;

public interface DomainEvent {
    String getEventId();
    LocalDateTime getOccurredAt();
}

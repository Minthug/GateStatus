package com.example.GateStatus.global.config.EventListner;

public interface EventPublisher {
    void publish(DomainEvent event);
}

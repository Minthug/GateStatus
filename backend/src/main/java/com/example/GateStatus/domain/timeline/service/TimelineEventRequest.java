package com.example.GateStatus.domain.timeline.service;

import com.example.GateStatus.domain.timeline.TimelineEventType;

import java.time.LocalDate;

public record TimelineEventRequest(String sourceType, String sourceId, Long figureId,
                                   String title, String description, LocalDate eventDate, TimelineEventType eventType) {

    public String getDisplayName() {
        return eventType != null ? eventType.getDisplayName() : null;
    }
}

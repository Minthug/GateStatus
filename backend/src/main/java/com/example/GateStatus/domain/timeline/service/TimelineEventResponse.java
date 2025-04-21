package com.example.GateStatus.domain.timeline.service;

import com.example.GateStatus.domain.timeline.TimelineEventDocument;
import com.example.GateStatus.domain.timeline.TimelineEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record TimelineEventResponse(String id, Long figureId, String figureName,
                                    LocalDate eventDate, String title, String description,
                                    TimelineEventType eventType, String sourceType, String sourceId,
                                    String imageUrl, Map<String, Object> additionalDate,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static TimelineEventResponse from(TimelineEventDocument document) {
        return new TimelineEventResponse(
                document.getId(),
                document.getFigureId(),
                document.getFigureName(),
                document.getEventDate(),
                document.getTitle(),
                document.getDescription(),
                document.getEventType(),
                document.getSourceType(),
                document.getSourceId(),
                document.getImageUrl(),
                document.getAdditionalData(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}

package com.example.GateStatus.domain.timeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "timeline_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "figure_date_idx", def = "{'figureId':1. 'eventDate': -1}")
public class TimelineEventDocument {

    @Id
    private String id;

    @Indexed
    private Long figureId;

    private String figureName;

    @Indexed
    private LocalDate eventDate;

    private String title;

    private String description;

    @Indexed
    private TimelineEventType eventType;

    private String sourceType;

    private String sourceId;

    private String imageUrl;

    private Map<String, Object> additionalData;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}

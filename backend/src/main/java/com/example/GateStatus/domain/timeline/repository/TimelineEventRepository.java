package com.example.GateStatus.domain.timeline.repository;

import com.example.GateStatus.domain.timeline.TimelineEventDocument;
import com.example.GateStatus.domain.timeline.TimelineEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TimelineEventRepository extends MongoRepository<TimelineEventDocument, String> {

    Page<TimelineEventDocument> findByFigureIdOrderByEventDateDesc(Long figureId, Pageable pageable);

    Page<TimelineEventDocument> findByFigureIdAndEventTypeOrderByEventDateDesc(Long figureId, TimelineEventType eventType, Pageable pageable);

}

package com.example.GateStatus.domain.timeline.repository;

import com.example.GateStatus.domain.timeline.TimelineEventDocument;
import com.example.GateStatus.domain.timeline.TimelineEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TimelineEventRepository extends MongoRepository<TimelineEventDocument, String> {

    // 특정 정치인의 타임라인 조회(시간 역순)
    Page<TimelineEventDocument> findByFigureIdOrderByEventDateDesc(Long figureId, Pageable pageable);

    // 특정 정치인과 이벤트 타입으로 필터링
    Page<TimelineEventDocument> findByFigureIdAndEventTypeOrderByEventDateDesc(Long figureId, TimelineEventType eventType, Pageable pageable);

    // 특정 기간 내 정치인의 타임라인 조회
    @Query("{'figureId': ?0, 'eventDate': {$gte: ?1, $lte: ?2}}")
    Page<TimelineEventDocument> findByFigureIdAndDateRange(Long figureId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 특정 키워드 포함하는 타임라인 검색
    @Query("{'figureId': ?0, $or: [{'title': {$regex: ?1, $options: 'i'}}, {'description': {$regex: ?1, $options: 'i'}}]}")
    Page<TimelineEventDocument> searchByKeyword(Long figureId, String keyword, Pageable pageable);

    // 특정 소스 타입(STATEMENT, BILL 등)으로 필터링
    Page<TimelineEventDocument> findByFigureIdAndSourceTypeOrderByEventDateDesc(Long figureId, String sourceType, Pageable pageable);

    // 원본 데이터 ID로 타임라인 이벤트 조회
    List<TimelineEventDocument> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    // 소스 ID로 이벤트 존재 여부 확인
    boolean existsBySourceTypeAndSourceId(String sourceType, String sourceId);
}

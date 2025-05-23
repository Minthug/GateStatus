package com.example.GateStatus.domain.timeline.controller;

import com.example.GateStatus.domain.timeline.TimelineEventType;
import com.example.GateStatus.domain.timeline.service.TimelineEventRequest;
import com.example.GateStatus.domain.timeline.service.TimelineEventResponse;
import com.example.GateStatus.domain.timeline.service.TimelineService;
import com.example.GateStatus.global.config.batch.BatchProcessResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/timeline")
@RequiredArgsConstructor
@Slf4j
public class TimelineController {

    private final TimelineService timelineService;

    /**
     * 특정 정치인의 타임라인 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @GetMapping("/{figureId}")
    public ResponseEntity<Page<TimelineEventResponse>> getFigureTimeline(@PathVariable Long figureId,
                                                                   @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인 타임라인 조회 요청: {}", figureId);
        Page<TimelineEventResponse> timeline = timelineService.getFigureTimeline(figureId, pageable);
        return ResponseEntity.ok(timeline);
    }

    /**
     * 특정 정치인의 타임라인 조회 (이벤트 타입 필터)
     * @param figureId
     * @param eventType
     * @param pageable
     * @return
     */
    @GetMapping("/{figureId}/type/{eventType}")
    public ResponseEntity<Page<TimelineEventResponse>> getFigureTimelineByType(@PathVariable Long figureId,
                                                                               @PathVariable TimelineEventType eventType,
                                                                               @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인 타임라인 조회 요청 (타입 필터): {}, 타입 {}", figureId, eventType);
        Page<TimelineEventResponse> timeline = timelineService.getFigureTimelineByType(figureId, eventType, pageable);
        return ResponseEntity.ok(timeline);
    }

    /**
     * 특정 기간 내 정치인의 타임라인 조회
     * @param figureId
     * @param startDate
     * @param endDate
     * @param pageable
     * @return
     */
    @GetMapping("/{figureId}/period")
    public ResponseEntity<Page<TimelineEventResponse>> getFigureTimelineByDateRange(@PathVariable Long figureId,
                                                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                                    @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인 타임라인 조회 요청 (기간 필터): {}. 기간: {} ~ {}", figureId, startDate, endDate);

        Page<TimelineEventResponse> timeline = timelineService.getFigureTimelineByDateRange(figureId, startDate, endDate, pageable);
        return ResponseEntity.ok(timeline);
    }

    /**
     * 특정 키워드를 포함한 타임라인 검색
     * @param figureId
     * @param keyword
     * @param pageable
     * @return
     */
    @GetMapping("/{figureId}/search")
    public ResponseEntity<Page<TimelineEventResponse>> searchFigureTimeline(@PathVariable Long figureId,
                                                                            @RequestParam String keyword,
                                                                            @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인 타임라인 검 요청: {}, 키워드 = {}", figureId, keyword);
        Page<TimelineEventResponse> timeline = timelineService.searchFigureTimeline(figureId, keyword, pageable);
        return ResponseEntity.ok(timeline);
    }

    /**
     * 타임라인 이벤트 추가 (통합 엔드포인트)
     * @param request
     * @return
     */
    @PostMapping("/events")
    public ResponseEntity<TimelineEventResponse> addTimelineEvent(@Valid @RequestBody TimelineEventRequest request) {
        log.info("타임라인 이벤트 추가 요청: 소스타입={}, 정치인 ID={}", request.sourceType(), request.figureId());

        TimelineEventResponse response;
        switch (request.sourceType()) {
            case "STATEMENT":
                response = timelineService.addStatementToTimeline(request.sourceId());
                break;
            case "BILL":
                response = timelineService.addBillToTimeline(request.sourceId(), request.figureId());
                break;
            case "CUSTOM":
                response = timelineService.addCustomEvent(
                        request.figureId(),
                        request.title(),
                        request.description(),
                        request.eventDate(),
                        request.eventType());
                break;
            default:
                return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 타임라인 삭제
     * @param eventId
     * @return
     */
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteTimelineEvent(@PathVariable String eventId) {
        log.info("타임라인 이벤트 삭제 요청: {}", eventId);
        timelineService.deleteTimelineEvent(eventId);
        return ResponseEntity.noContent().build();

    }
}

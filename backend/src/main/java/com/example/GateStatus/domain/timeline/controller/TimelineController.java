package com.example.GateStatus.domain.timeline.controller;

import com.example.GateStatus.domain.timeline.TimelineEventType;
import com.example.GateStatus.domain.timeline.service.TimelineEventResponse;
import com.example.GateStatus.domain.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.Path;

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

    @GetMapping("/{figureId}/period")
    public ResponseEntity<Page<TimelineEventResponse>> getFigureTimelineByDateRange(@PathVariable Long figureId,
                                                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                                    @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인 타임라인 조회 요청 (기간 필터): {}. 기간: {} ~ {}", figureId, startDate, endDate);

        Page<TimelineEventResponse> timeline = timelineService.getFigureTimelineByDateRange(figureId, startDate, endDate, pageable);
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/{figureId}/search")
    public ResponseEntity<Page<TimelineEventResponse>> searchFigureTimeline(@PathVariable Long figureId,
                                                                            @RequestParam String keyword,
                                                                            @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인 타임라인 검 요청: {}, 키워드 = {}", figureId, keyword);
        Page<TimelineEventResponse> timeline = timelineService.searchFigureTimeline(figureId, keyword, pageable);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/add/{statementId}")
    public ResponseEntity<TimelineEventResponse> addStatementToTimeline(@PathVariable String statementId) {
        log.info("발언 타임라인 추가 요청: {}", statementId);
        TimelineEventResponse response = timelineService.addStatementToTimeline(statementId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add/bill")
    public ResponseEntity<TimelineEventResponse> addBillToTimeline(@RequestParam Long billId,
                                                                   @RequestParam Long figureId) {
        log.info("법안 타임라인 추가 요청: 법안 ID={}, 정치인 ID={}", billId, figureId);
        TimelineEventResponse response = timelineService.addBillToTimeline(billId, figureId);
        return ResponseEntity.ok(response);
    }

}

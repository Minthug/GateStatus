package com.example.GateStatus.domain.timeline.controller;

import com.example.GateStatus.domain.timeline.service.TimelineEventResponse;
import com.example.GateStatus.domain.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/timeline")
@RequiredArgsConstructor
@Slf4j
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/{figureId}")
    public ResponseEntity<Page<TimelineEventResponse>> getFigureTimeline(@PathVariable Long figureId,
                                                                   @PageableDefault(value = 10) Pageable pageable) {
        log.info("정치인 타임라인 조회 요청: {}", figureId);
        Page<TimelineEventResponse> timeline = timelineService.getFigureTimeline(figureId, pageable);
        return ResponseEntity.ok(timeline);
    }
}

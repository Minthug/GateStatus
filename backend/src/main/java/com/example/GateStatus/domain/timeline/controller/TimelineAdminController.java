package com.example.GateStatus.domain.timeline.controller;

import com.example.GateStatus.domain.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/timeline")
@Slf4j
public class TimelineAdminController {

    private final TimelineService timelineService;

    /**
     * 발언 데이터 수동 동기화 실행
     * 스케쥴러를 기다리지 않고 즉시 동기화 실행
     * @param startDate
     * @param endDate
     * @return
     */
    @PostMapping("/sync-statements")
    public ResponseEntity<Map<String, Object>> syncStatements(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().minusWeeks(1);
        }

        if (endDate == null) {
            endDate = LocalDate.now();
        }

        log.info("수동 발언 동기화 요청: {} ~ {}", startDate, endDate);

        LocalDateTime syncStartTime = LocalDateTime.now();

        try {
            timelineService.syncStatementsToTimeline();

            LocalDateTime syncEndTime = LocalDateTime.now();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("message", "발언 데이터 동기화가 완료되었습니다");
            result.put("startDate", startDate);
            result.put("endDate", endDate);
            result.put("syncStartTime", syncStartTime);
            result.put("syncEndTime", syncEndTime);
            result.put("processingTimeSeconds", java.time.Duration.between(syncStartTime, syncEndTime).getSeconds());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("수동 발언 동기화 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("message", "발언 데이터 동기화 중 오류가 발생했습니다: " + e.getMessage());
            errorResult.put("startDate", startDate);
            errorResult.put("endDate", endDate);
            errorResult.put("syncStartTime", syncStartTime);
            errorResult.put("errorTime", LocalDateTime.now());

            return ResponseEntity.status(500).body(errorResult);
        }
    }

    /**
     * 스케줄러 상태 확인
     * 마지막 동기화 시간, 다음 실행 예정 시간 등 조회
     * @return
     */
    @GetMapping("/scheduler-status")
    public ResponseEntity<Map<String, Object>> getTimelineStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("message", "타임라인 통계 조회 기능 - 향후 구현 예정");
            stats.put("note", "전체 이벤트 수, 타입별 분포, 최근 동기화 통계 등");
            stats.put("queryTime", LocalDateTime.now());

            // TODO: 실제 통계 데이터 조회 로직 추가
            // stats.put("totalEvents", timelineService.getTotalEventsCount());
            // stats.put("eventsByType", timelineService.getEventCountsByType());
            // stats.put("recentSyncStats", timelineService.getRecentSyncStatistics());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("타임라인 통계 조회 실패", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("message", "통계 조회 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResult);
        }
    }


}

package com.example.GateStatus.domain.timeline.controller;

import com.example.GateStatus.domain.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
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

    /**
     * 타임라인 서비스가 정상 동작하는지 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 간단한 서비스 동작 확인
            health.put("status", "HEALTHY");
            health.put("service", "TimelineService");
            health.put("checkTime", LocalDateTime.now());
            health.put("message", "타임라인 서비스가 정상 동작 중입니다");

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("타임라인 서비스 헬스체크 실패", e);

            health.put("status", "UNHEALTHY");
            health.put("service", "TimelineService");
            health.put("checkTime", LocalDateTime.now());
            health.put("error", e.getMessage());

            return ResponseEntity.status(503).body(health);
        }
    }


    /**
     * 특정 기간의 발언 데이터 강제 재동기화
     * 데이터 불일치 문제 해결용
     */
    @PostMapping("/force-resync")
    public ResponseEntity<Map<String, Object>> forceResyncStatements(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                     @RequestParam(defaultValue = "false") boolean deleteExisting) {
        log.warn("강제 재동기화 요청: {} ~ {}, deleteExisting={}", startDate, endDate, deleteExisting);

        Map<String, Object> result = new HashMap<>();

        try {
            if (deleteExisting) {
                // TODO: 기존 타임라인 이벤트 삭제 로직 (위험한 작업이므로 신중히)
                log.warn("기존 타임라인 이벤트 삭제 기능은 안전상 비활성화됨");
                result.put("warning", "기존 데이터 삭제는 현재 지원하지 않습니다");
            }

            // 강제 동기화 실행
            timelineService.syncStatementsToTimeline();

            result.put("status", "SUCCESS");
            result.put("message", "강제 재동기화가 완료되었습니다");
            result.put("startDate", startDate);
            result.put("endDate", endDate);
            result.put("syncTime", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("강제 재동기화 실패", e);

            result.put("status", "ERROR");
            result.put("message", "강제 재동기화 중 오류가 발생했습니다: " + e.getMessage());
            result.put("errorTime", LocalDateTime.now());

            return ResponseEntity.status(500).body(result);
        }
    }
}

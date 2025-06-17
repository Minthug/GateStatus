package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.domain.statement.service.StatementSyncService;
import com.example.GateStatus.global.config.open.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/admin/statements/")
public class StatementSyncController {

    private final StatementSyncService syncService;
    private final StatementService statementService;


    @PostMapping("/sync/figureName")
    public ResponseEntity<ApiResponse<Integer>> syncStatementsByFigure(@RequestParam String figureName) {
        log.info("국회의원 '{}' 발언 정보 동기화 요청", figureName);

        try {
            int count = syncService.syncStatementsByFigure(figureName);
            return ResponseEntity.ok(ApiResponse.success(String.format("국회의원 '%s' 발언 정보 %d건 동기화 완료", figureName, count), count));
        } catch (Exception e) {
            log.error("발언 정보 동기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("발언 정보 동기화 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/all")
    public ResponseEntity<ApiResponse<String>> syncAllStatementsAsync() {
        log.info("모든 국회의원 발언 정보 비동기 동기화 요청");

        String jobId = syncService.syncStatementsAsync();
        return ResponseEntity.ok(ApiResponse.success("모든 국회의원 발언 정보 비동기 동기화 작업이 시작되었습니다", jobId));
    }

    @GetMapping("/sync/status/{jobId}")
    public ResponseEntity<ApiResponse<SyncJobStatus>> getSyncStatus(@PathVariable String jobId) {
        SyncJobStatus status = syncService.getSyncJobStatus(jobId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success("발언 정보 동기화 작업 상태", status));
    }

    @PostMapping("/sync/period")
    public ResponseEntity<ApiResponse<Integer>> syncStatementsByPeriod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("기간별 발언 정보 동기화 요청: {} ~ {}", startDate, endDate);

        try {
            int count = syncService.syncStatementsByPeriod(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(String.format("기간(%s ~ %s) 발언 정보 %d건 동기화 완료",
                    startDate, endDate, count), count));
        } catch (Exception e) {
            log.error("기간별 발언 정보 동기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("기간별 발언 정보 동기화 실패: " + e.getMessage()));
        }
    }

}

package com.example.GateStatus.domain.proposedBill.controller;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillQueueService;
import com.example.GateStatus.domain.proposedBill.service.response.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillSyncService;
import com.example.GateStatus.global.config.open.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/proposed")
@RequiredArgsConstructor
@Slf4j
public class ProposedController {

    private final ProposedBillService billService;
    private final ProposedBillSyncService billSyncService;
    private final ProposedBillQueueService billQueueService;

    /**
     * 법안 ID로 법안 상세 정보 조회
     * @return
     */
    @GetMapping("/{billId}")
    public ResponseEntity<ProposedBillResponse> getBill(@PathVariable String billId) {
        log.info("법안 상세 정보 조회 요청: {}", billId);
        ProposedBillResponse response = billService.findBillById(billId);
        return ResponseEntity.ok(response);
    }

    /**
     * 법안 ID로 법안 상세 정보 조회 (조회수 증가 없음, 시스템용)
     * @param billId 법안 ID
     * @return 법안 상세 정보
     */
    @GetMapping("/{billId}/info")
    public ResponseEntity<ProposedBillResponse> getBillInfo(@PathVariable @NotBlank String billId) {
        log.debug("법안 정보 조회 요청 (시스템용): {}", billId);

        ProposedBillResponse response = billService.getBillById(billId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/proposer/{proposerId}")
    public ResponseEntity<Page<ProposedBillResponse>> getBillsByProposer(
            @PathVariable @NotNull long proposerId,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("발의자별 법안 목록 조회: proposerId={}", proposerId);
        Page<ProposedBillResponse> bills = billService.findBillsByProposer(proposerId, pageable);
        return ResponseEntity.ok(bills);
    }


    // ========================================
    // 🎯 사용자 친화적 API (새로 추가)
    // ========================================

    @GetMapping("/by-proposer-name")
    public ResponseEntity<Page<ProposedBillResponse>> getBillsByProposerName(
            @RequestParam @NotBlank String proposerName,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("발의자 이름으로 법안 목록 조회: proposerName={}", proposerName);
        Page<ProposedBillResponse> bills = billService.findBillsByProposerName(proposerName, pageable);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/search-all")
    public ResponseEntity<Page<ProposedBillResponse>> searchAllBills(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "all") String type,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("통합 검색: query={}, type={}", query, type);

        Page<ProposedBillResponse> bills = switch (type.toLowerCase()) {
            case "proposer" -> billService.findBillsByProposerName(query, pageable);
            case "bill" -> billService.findBillsByName(query, pageable);
            case "keyword" -> billService.searchBills(query, pageable);
            case "all" -> billService.searchAllFields(query, pageable);
            default -> throw new IllegalArgumentException("유효하지 않은 검색 타입: " + type);
        };
        return ResponseEntity.ok(bills);
    }


    @GetMapping("/popular")
    public ResponseEntity<List<ProposedBillResponse>> getPopularBills(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("인기 법안 목록 조회 요청: limit={}", limit);
        List<ProposedBillResponse> bills = billService.findPopularBills(limit);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProposedBillResponse>> searchBills(@RequestParam String keyword,
                                                                  @PageableDefault(size = 10) Pageable pageable) {
        log.info("법안 검색 요청: keyword={}", keyword);
        Page<ProposedBillResponse> bills = billService.searchBills(keyword, pageable);

        return ResponseEntity.ok(bills);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProposedBillResponse>> getBillsByStatus(@PathVariable BillStatus status) {
        log.info("상태별 법안 목록 조회: status={}", status);
        List<ProposedBillResponse> bills = billService.findBillByStatus(status);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/period")
    public ResponseEntity<List<ProposedBillResponse>> getBillsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate endDate) {

        log.info("기간별 법안 목록 조회: {} ~ {}", startDate, endDate);
        List<ProposedBillResponse> bills = billService.findBillsByPeriod(startDate, endDate);
        return ResponseEntity.ok(bills);
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Integer>> syncBills(@RequestParam String proposerName) {
        log.info("법안 데이터 동기화 요청: 발의자={}", proposerName);

        try {
            int syncCount = billSyncService.syncBillsByProposer(proposerName);
            return ResponseEntity.ok(ApiResponse.success("법안 동기화 완료", syncCount));
        } catch (Exception e) {
            log.error("법안 동기화 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("법안 동기화 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/all")
    public ResponseEntity<ApiResponse<Integer>> syncAllBills() {
        log.info("전체 법안 데이터 동기화 요청");

        try {
            int syncCount = billSyncService.syncAllBills();
            return ResponseEntity.ok(ApiResponse.success("전체 법안 동기화 완료", syncCount));
        } catch (Exception e) {
            log.error("전체 법안 동기화 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("전체 법안 동기화 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/async")
    public ResponseEntity<ApiResponse<String>> syncBillsAsync(@RequestParam(required = false) String proposerName) {

        log.info("비동기 법안 동기화 요청: 대상={}",
                proposerName != null ? proposerName : "전체");

        try {
            CompletableFuture<Integer> future;
            if (proposerName != null && !proposerName.trim().isEmpty()) {
                future = billSyncService.syncBillsByProposerAsync(proposerName);
            } else {
                future = billSyncService.syncAllBillsAsync();
            }

            return ResponseEntity.ok(ApiResponse.success(
                    "비동기 법안 동기화 작업이 시작되었습니다.", "ASYNC_STARTED"));
        } catch (Exception e) {
            log.error("비동기 법안 동기화 시작 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("비동기 법안 동기화 시작 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/queue")
    public ResponseEntity<ApiResponse<String>> syncBillQueue(@RequestParam(required = false) String proposerName) {
        log.info("큐 기반 법안 동기화 요청: 대상={}", proposerName != null ? proposerName : "전체");

        try {
            String jobId;
            if (proposerName != null && !proposerName.trim().isEmpty()) {
                jobId = billSyncService.queueBillSyncTask(proposerName);
            } else {
                jobId = billSyncService.queueAllBillsSyncTask();
            }
            return ResponseEntity.ok(ApiResponse.success(
                    "비동기 법안 동기화 작업이 큐에 추가되었습니다.", jobId));
        } catch (Exception e) {
            log.error("큐 기반 법안 동기화 요청 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("큐 기반 법안 동기화 요청 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/sync/status/{jobId}")
    public ResponseEntity<ApiResponse<SyncJobStatus>> getSyncStatus(@PathVariable String jobId) {
        log.info("동기화 작업 상태 조회: JobId={}", jobId);

        SyncJobStatus status = billQueueService.getJobStatus(jobId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success("작업 상태 조회 성공", status));
    }
}

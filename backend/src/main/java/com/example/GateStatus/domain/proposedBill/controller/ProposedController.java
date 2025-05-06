package com.example.GateStatus.domain.proposedBill.controller;

import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.global.config.open.ApiResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/proposed")
@RequiredArgsConstructor
@Slf4j
public class ProposedController {

    private final ProposedBillService proposedBillService;
    private final ProposedBillApiService apiService;

    /**
     * 법안 ID로 법안 상세 정보 조회
     * @param proposedId
     * @return
     */
    @GetMapping("/{proposedId}")
    public ResponseEntity<ProposedBillResponse> getProposedId(@PathVariable Long proposedId) {
        log.info("법안 상세 정보 조회 요청: {}", proposedId);
        return ResponseEntity.ok(proposedBillService.findBillById(proposedId));
    }

    /**
     * 특정 국회의원이 발의한 법안 목록 조회
     * @param proposerId
     * @param pageable
     * @return
     */
    @GetMapping("/proposer/{proposerId}")
    public ResponseEntity<Page<ProposedBillResponse>> getBillsByProposer(@PathVariable Long proposerId,
                                                                         @PageableDefault(size = 10) Pageable pageable) {
        log.info("발의자별 법안 목록 조회: {}", proposerId);
        Page<ProposedBillResponse> bills = proposedBillService.findBillsByProposer(proposerId, pageable);

        return ResponseEntity.ok(bills);
    }


    /**
     * 인기 법안 목록 조회
     * @param limit
     * @return
     */
    @GetMapping("/popular")
    public ResponseEntity<List<ProposedBillResponse>> getPopularBills(@RequestParam(defaultValue = "10") int limit) {
        log.info("인기 법안 목록 조회 요청: {}", limit);
        List<ProposedBillResponse> bills = proposedBillService.findPopularBills(limit);
        return ResponseEntity.ok(bills);
    }

    /**
     * 키워드로 법안 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ProposedBillResponse>> searchBills(@RequestParam String keyword,
                                                                  @PageableDefault(size = 10) Pageable pageable) {
        log.info("법안 검색 요청: keyword = {}", keyword);
        Page<ProposedBillResponse> bills = proposedBillService.searchBills(keyword, pageable);
        return ResponseEntity.ok(bills);
    }

    /**
     * 특정 상태의 법안 목록 조회
     * @param status
     * @return
     */
    @GetMapping("/v1/status/{status}")
    public ResponseEntity<List<ProposedBillResponse>> findProposedByStatus(@PathVariable BillStatus status) {
        log.info("상태별 법안 목록 조회: {}", status);

        List<ProposedBillResponse> bills = proposedBillService.findBillByStatus(status);
        return ResponseEntity.ok(bills);
    }

    /**
     * 기간별 법안 목록 조회
     * @param startDate
     * @param endDate
     * @return
     */
    @GetMapping("/period")
    public ResponseEntity<List<ProposedBillResponse>> getBillsByPeriod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("기간별 법안 목록 조회: {} ~ {}", startDate, endDate);
        List<ProposedBillResponse> bills = proposedBillService.findBillsByPeriod(startDate, endDate);
        return ResponseEntity.ok(bills);
    }

    /**
     * API 데이터 동기화 엔드포인트
     * @param proposerName
     * @return
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Integer>> syncBills(@RequestParam String proposerName) {
        log.info("법안 데이터 동기화 요청: 발의자 = {}", proposerName);

        try {
            int syncCount = proposedBillService.syncBillsByProposer(proposerName);
            return ResponseEntity.ok(ApiResponse.success("법안 동기화 완료", syncCount));
        } catch (Exception e) {
            log.error("법안 동기화 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("법안 동기화 실패: " + e.getMessage()));
        }

    }
}

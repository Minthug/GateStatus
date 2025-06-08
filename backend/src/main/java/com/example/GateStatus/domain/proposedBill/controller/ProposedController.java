package com.example.GateStatus.domain.proposedBill.controller;

import com.example.GateStatus.domain.proposedBill.service.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillSyncService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/proposed")
@RequiredArgsConstructor
@Slf4j
public class ProposedController {

    private final ProposedBillService billService;
    private final ProposedBillSyncService billSyncService;

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

    @GetMapping("/popular")
    public ResponseEntity<List<ProposedBillResponse>> getPopularBills(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("인기 법안 목록 조회 요청: limit={}", limit);
        List<ProposedBillResponse> bills = billService.findPopularBills(limit);
        return ResponseEntity.ok(bills);
    }
}

package com.example.GateStatus.domain.proposedBill.controller;

import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private final ProposedBillService proposedBillService;
    private final ProposedBillApiService apiService;

    @GetMapping("/{proposedId}")
    public ResponseEntity<ProposedBillResponse> getProposedId(@PathVariable Long proposedId) {
        log.info("법안 상세 정보 조회 요청: {}", proposedId);
        return ResponseEntity.ok(proposedBillService.findBillById(proposedId));
    }

    @GetMapping("/proposer/{proposerId}")
    public ResponseEntity<Page<ProposedBillResponse>> getBillsByProposer(@PathVariable Long proposerId,
                                                                         @PageableDefault(size = 10) Pageable pageable) {
        log.info("발의자별 법안 목록 조회: {}", proposerId);
        Page<ProposedBillResponse> bills = proposedBillService.findBillsByProposer(proposerId, pageable);

        return ResponseEntity.ok(bills);
    }

    @GetMapping("/v1/type/{proposedId}")
    public ResponseEntity<List<ProposedBillResponse>> findProposedByStatus(@PathVariable BillStatus status) {
        return ResponseEntity.ok(proposedBillService.findBillByStatus(status));
    }
}

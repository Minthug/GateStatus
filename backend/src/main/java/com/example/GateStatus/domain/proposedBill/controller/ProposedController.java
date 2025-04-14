package com.example.GateStatus.domain.proposedBill.controller;

import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/proposed")
@RequiredArgsConstructor
public class ProposedController {

    private final ProposedBillService proposedBillService;
    private final ProposedBillApiService apiService;

    @GetMapping("/{proposedId}")
    public ResponseEntity<ProposedBillResponse> getProposedId(@PathVariable Long proposedId) {
        return ResponseEntity.ok(proposedBillService.findBillById(proposedId));
    }

//    @GetMapping
//    public ResponseEntity<Page<ProposedBillResponse>> findAllProposed(@RequestParam(defaultValue = "0") int page,
//                                                                      @RequestParam(defaultValue = "0") int size,
//                                                                      @RequestParam(required = false) String type,
//                                                                      @RequestParam(required = false) String keyword) {
//
//        PageRequest pageRequest = PageRequest.of(page, size);
//        return ResponseEntity.ok(proposedBillService.findBillByStatus())
//
//    }
}

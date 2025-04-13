package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillService {

    private final ProposedBillRepository proposedBillRepository;
    private final ProposedBillApiService proposedBillApiService;


}

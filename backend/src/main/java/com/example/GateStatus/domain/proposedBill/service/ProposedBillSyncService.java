package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillSyncService {

    private final ProposedBillApiService billApiService;
    private final ProposedBillQueueService billQueueService;
    private final BillAsyncService billAsyncService;
    private final ProposedBillRepository billRepository;
    private final FigureRepository figureRepository;

    @Transactional
    public int syncBillsByProposer(String proposerName) {
        validateProposerName(proposerName);

        log.info("발의자 {}의 법안 동기화 시작", proposerName);

        int syncCount = billApiService.syncBillByProposer(proposerName);
        log.info("발의자 {}의 법안 동기화 완료: {}건", proposerName, syncCount);
        return syncCount;
    }

    private void validateProposerName(String proposerName) {
        if (proposerName == null || proposerName.trim().isEmpty()) {
            throw new IllegalArgumentException("발의자 이름은 필수입니다");
        }


    }
}

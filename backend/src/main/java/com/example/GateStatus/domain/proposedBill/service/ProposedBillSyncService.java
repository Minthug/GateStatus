package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.service.response.ProposedBillApiDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillSyncService {

    private final ProposedBillApiService billApiService;
    private final ProposedBillQueueService billQueueService;
    private final BillAsyncService billAsyncService;

    @Transactional
    public int syncBillsByProposer(String proposerName) {
        validateProposerName(proposerName);

        log.info("발의자 {}의 법안 동기화 시작", proposerName);
        int syncCount = billApiService.syncBillByProposer(proposerName);
        log.info("발의자 {}의 법안 동기화 완료: {}건", proposerName, syncCount);
        return syncCount;
    }

    @Transactional
    public int syncAllBills() {
        log.info("모든 국회의원이 법안 동기화 시작");
        int syncCount = billApiService.syncAllBills();
        log.info("모든 국회의원의 법안 동기화 완료: {}", syncCount);

        return syncCount;
    }

    public CompletableFuture<Integer> syncBillsByProposerAsync(String proposerName) {
        validateProposerName(proposerName);

        log.info("발의자 {}의 법안 비동기(@Async) 동기화 시작", proposerName);
        return billAsyncService.syncBillsByProposerAsync(proposerName);
    }

    public CompletableFuture<Integer> syncAllBillsAsync() {
        log.info("모든 국회의원의 법안 비동기(@Async) 동기화 시작");
        return billAsyncService.syncAllBillsAsync();
    }

    public String queueBillSyncTask(String proposerName) {
        validateProposerName(proposerName);

        String jobId = UUID.randomUUID().toString();
        log.info("발의자 {}의 법안 비동기(큐) 동기화 작업({}) 시작", proposerName, jobId);

        billQueueService.queueBillsSyncTask(proposerName, jobId);
        return jobId;
    }

    public String queueAllBillsSyncTask() {
        log.info("모든 국회의원의 법안 비동기(큐) 동기화 작업 시작");
        return billQueueService.queueAllBillsSyncTask();
    }

//    @Transactional
//    public ProposedBill updateFromApiData(String billId, ProposedBillApiDTO apiData) {
//        validateApiData(billId, apiData);
//
//        Optional<ProposedBill> existingBill = billRepository.findByBillId(billId);
//
//        if (existingBill.isPresent()) {
//            return updateExistingBill(existingBill.get(), apiData);
//        } else {
//            return createNewBill(billId, apiData);
//        }
//    }
//
//
//    private ProposedBill updateExistingBill(ProposedBill bill, ProposedBillApiDTO apiData) {
//        if (apiData.proposer() != null && !apiData.proposer().isEmpty()) {
//            Figure proposer = figureRepository.findByName(apiData.proposer()).orElse(null);
//            bill.setProposer(proposer);
//        }
//
//        LocalDate proposeDate = BillUtils.parseDate(apiData.proposedDate());
//        LocalDate processDate = BillUtils.parseDate(apiData.processDate());
//
//        bill.setBillNo(apiData.billNo());
//        bill.setBillName(apiData.billName());
//        bill.setProposeDate(proposeDate);
//        bill.setSummary(apiData.summary());
//        bill.setBillUrl(apiData.linkUrl());
//        bill.setProcessDate(processDate);
//        bill.setProcessResult(apiData.processResult());
//        bill.setProcessResultCode(apiData.processResultCode());
//        bill.setCommittee(apiData.committeeName());
//        bill.setBillStatus(BillUtils.determineBillStatus(apiData.processResult()));
//        bill.setCoProposers(apiData.coProposers());
//
//        return billRepository.save(bill);
//    }


    // === Private Validation Methods ===

    private void validateJobId(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("작업 ID는 필수입니다");
        }
    }

    private void validateProposerName(String proposerName) {
        if (proposerName == null || proposerName.trim().isEmpty()) {
            throw new IllegalArgumentException("발의자 이름은 필수입니다");
        }
    }

    private void validateApiData(String billId, ProposedBillApiDTO apiData) {
        if (billId == null || billId.trim().isEmpty()) {
            throw new IllegalArgumentException("법안 ID는 필수입니다");
        }
        if (apiData == null) {
            throw new IllegalArgumentException("API 데이터는 필수입니다");
        }
    }

    public SyncJobStatus getJobStatus(String jobId) {
        validateJobId(jobId);
        return billQueueService.getJobStatus(jobId);
    }
}

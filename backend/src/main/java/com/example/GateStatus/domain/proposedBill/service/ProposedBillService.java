package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillService {

    private final FigureRepository figureRepository;
    private final ProposedBillRepository billRepository;
    private final ProposedBillApiService proposedBillApiService;
    private final ProposedBillQueueService proposedBillQueueService;
    private final BillAsyncService billAsyncService;

    /**
     * 법안 ID로 법안 상세 정보 조회
     * @param id
     * @return
     */
    @Transactional
    public ProposedBillResponse findBillById(String id) {
        ProposedBill bill = billRepository.findByBillId(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 법안이 존재하지 않습니다: " + id));
//        bill.incrementViewCount();

        return ProposedBillResponse.from(bill);
    }

    /**
     * 특정 국회의원이 발의한 법안 목록 조회
     * @param proposerId
     * @param pageable
     * @return
     */
    @Transactional
    public Page<ProposedBillResponse> findBillsByProposer(Long proposerId, Pageable pageable) {
        Figure proposer = figureRepository.findById(proposerId)
                .orElseThrow(() -> new EntityNotFoundException("해당 국회의원이 존재하지 않습니다: " + proposerId));

        Page<ProposedBill> bills = billRepository.findByProposer(proposer, pageable);
        return bills.map(ProposedBillResponse::from);
    }

    /**
     * 인기 법안 목록 조회
     * @param limit
     * @return
     */
    @Transactional(readOnly = true)
    public List<ProposedBillResponse> findPopularBills(int limit) {
        return billRepository.findTopByOrderByViewCountDesc(PageRequest.of(0, limit))
                .stream()
                .map(ProposedBillResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 키워드로 법안 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<ProposedBillResponse> searchBills(String keyword, Pageable pageable) {
        Page<ProposedBill> bills = billRepository.findByBillNameContaining(keyword, pageable);
        return bills.map(ProposedBillResponse::from);
    }

    /**
     * 특정 상태의 법안 목록 조회
     * @param status
     * @return
     */
    @Transactional(readOnly = true)
    public List<ProposedBillResponse> findBillByStatus(BillStatus status) {
        return billRepository.findByBillStatus(status)
                .stream()
                .map(ProposedBillResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 법안 목록 조회
     * @param startDate
     * @param endDate
     * @return
     */
    @Transactional(readOnly = true)
    public List<ProposedBillResponse> findBillsByPeriod(LocalDate startDate, LocalDate endDate) {
        return billRepository.findByPeriod(startDate, endDate)
                .stream()
                .map(ProposedBillResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * API 데이터 동기화 메서드(퍼사드 패턴)
     * @param proposerName
     * @return
     */
    @Transactional
    public int syncBillsByProposer(String proposerName) {

        log.info("발의자 {}의 법안 동기화 시작", proposerName);

        int syncCount = proposedBillApiService.syncAllBills();
        log.info("발의자 {}의 법안 동기화 완료: {}건", proposerName, syncCount);
        return syncCount;
    }

    /**
     * 비동기 법안 동기화 메서드(@Async 활용)
     * @param proposerName
     * @return
     */
    public CompletableFuture<Integer> syncBillsByProposerAsync(String proposerName) {
        log.info("발의자 {}의 법안 비동기(@Async) 동기화 시작", proposerName);
        return billAsyncService.syncBillsByProposerAsync(proposerName);
    }

    /**
     * 모든 국회의원 법안 비동기 동기화 메서드 (@Async 활용)
     * @return
     */
    public CompletableFuture<Integer> syncAllBillsAsync() {
        log.info("모든 국회의원의 법안 비동기(@Async) 동기화 시작");
        return billAsyncService.syncAllBillsAsync();
    }

    /**
     * 비동기 법안 동기화 메서드 (메시지 큐 활용)
     * @param proposerName
     * @return
     */
    public String queueBillSyncTask(String proposerName) {
        String jobId = UUID.randomUUID().toString();
        log.info("발의자 {}의 법안 비동기(큐) 동기화 작업({}) 시작", proposerName, jobId);
        proposedBillQueueService.queueBillsSyncTask(proposerName, jobId);
        return jobId;
    }

    /**
     * 모든 국회의원 법안 비동기 동기화 메서드 (메시지 큐 활용)
     * @return
     */
    public String queueAllBillsSyncTask() {
        log.info("모든 국회의원의 법안 비동기(큐) 동기화 작업 시작");
        return proposedBillQueueService.queueAllBillsSyncTask();
    }

    /**
     * 작업 상태 조회 메서드
     * @param jobId
     * @return
     */
    public ProposedBillQueueService.JobStatus getJobStatus(String jobId) {
        return proposedBillQueueService.getJobStatus(jobId);
    }

    @Transactional
    public ProposedBill updateFromApiData(String billId, ProposedBillApiDTO apiData) {
        ProposedBill bill = billRepository.findByBillId(billId)
                .orElseGet(() -> ProposedBill.builder()
                        .billId(billId)
                        .billName(apiData.billName())
                        .billNo(apiData.billNo())
                        .build());

        if (apiData.proposer() != null && !apiData.proposer().isEmpty()) {
            Figure proposer = figureRepository.findByName(apiData.proposer()).orElse(null);
            bill.setProposer(proposer);
        }

        LocalDate proposeDate = parseDate(apiData.proposedDate());
        LocalDate processDate = parseDate(apiData.processDate());

        ProposedBill updateBill = ProposedBill.builder()
                .billId(billId)
                .billNo(apiData.billNo())
                .billName(apiData.billName())
                .proposeDate(proposeDate)
                .summary(apiData.summary())
                .billUrl(apiData.linkUrl())
                .processDate(processDate)
                .processResult(apiData.processResult())
                .processResultCode(apiData.processResultCode())
                .committee(apiData.committeeName())
                .billStatus(determineBillStatus(apiData.processResult()))
                .build();

        updateBill.setCoProposers(apiData.coProposers());
        return billRepository.save(updateBill);
    }

    private BillStatus determineBillStatus(String processResult) {
        if (processResult == null || processResult.isEmpty()) {
            return BillStatus.PROPOSED;
        } else if (processResult.contains("원안가결") || processResult.contains("수정가결")) {
            return BillStatus.PASSED;
        } else if (processResult.contains("폐기") || processResult.contains("부결")) {
            return BillStatus.REJECTED;
        } else if (processResult.contains("대안반영")) {
            return BillStatus.ALTERNATIVE;
        } else if (processResult.contains("철회")) {
            return BillStatus.WITHDRAWN;
        } else {
            return BillStatus.PROCESSING;
        }
    }

    /**
     * 날짜 문자열 파싱
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            log.warn("날짜 변환 실패: {}", dateStr);
            return null;
        }
    }
}

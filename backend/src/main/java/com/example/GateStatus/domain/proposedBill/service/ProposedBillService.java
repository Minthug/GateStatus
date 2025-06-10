package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.proposedBill.service.response.ProposedBillResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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
     * 법안 ID로 법안 상세 정보 조회 (조회수 증가)
     */
    @Transactional
    public ProposedBillResponse findBillById(String id) {
        ProposedBill bill = getBillByIdOrThrow(id);
        bill.incrementViewCount();
        billRepository.save(bill);

        log.debug("법안 조회 및 조회수 증가: ID={}, 현재 조회수={}", id, bill.getViewCount());
        return ProposedBillResponse.from(bill);
    }

    /**
     * 법안 ID로 법안 상세 정보 조회 (조회수 증가 없음)
     * 시스템 내부 사용
     */
    @Transactional(readOnly = true)
    public ProposedBillResponse getBillById(String id) {
        ProposedBill bill = getBillByIdOrThrow(id);
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
        Figure proposer = getFigureByIdOrThrow(proposerId);
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
        validateLimit(limit);

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
        validateKeyword(keyword);

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
        validateStatus(status);

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
        validateDateRange(startDate, endDate);

        return billRepository.findByPeriod(startDate, endDate)
                .stream()
                .map(ProposedBillResponse::from)
                .collect(Collectors.toList());
    }

    // === Private Helper Methods ===

    private ProposedBill getBillByIdOrThrow(String id) {
        return billRepository.findByBillId(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 법안이 존재하지 않습니다: " + id));
    }

    private Figure getFigureByIdOrThrow(Long proposerId) {
        return figureRepository.findById(proposerId)
                .orElseThrow(() -> new EntityNotFoundException("해당 국회의원이 존재하지 않습니다 " + proposerId));
    }


    private void validateLimit(int limit) {
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("조회 개수는 1~100 사이여야 합니다: " + limit);
        }
    }

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드는 필수 입니다");
        }
        if (keyword.length() < 2) {
            throw new IllegalArgumentException("검색 키워드는 2자 이상 이어야 합니다");
        }
    }

    private void validateStatus(BillStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("법안 상태는 필수입니다");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일과 종료일은 필수입니다");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다");
        }
    }
}

package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillService {

    private final FigureRepository figureRepository;
    private final ProposedBillRepository billRepository;
    private final ProposedBillApiService proposedBillApiService;

    /**
     * 법안 ID로 법안 상세 정보 조회
     * @param id
     * @return
     */
    @Transactional
    public ProposedBillResponse findBillById(Long id) {
        ProposedBill bill = billRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 법안이 존재하지 않습니다: " + id));

        bill.incrementViewCount();

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


}

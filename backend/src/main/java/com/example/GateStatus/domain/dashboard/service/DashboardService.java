package com.example.GateStatus.domain.dashboard.service;

import com.example.GateStatus.domain.dashboard.dto.response.*;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FigureRepository figureRepository;
    private final ProposedBillRepository billRepository;
    private final StatementMongoRepository statementMongoRepository;
    private final VoteRepository voteRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(String figureId) {
        // 1. 정치인 정보 조회
        Figure figure = figureRepository.findByFigureId(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + figureId));

        // 2. 법안 통계 조회
        BillStatistics billStats = getBillStatistics(figure.getId());

        // 3. 발언 통계 조회
        StatementStatistics statementStats = getStatementStatistics(figure.getId());

        // 4. 투표 통계 조회
        VoteStatistics voteStats = getVoteStatistics(figure.getId());

        // 5. 월별 법안 발의 추의 조회
        List<BillOverTimeDTO> billsOverTime = getBillOverTime(figure.getId());

        // 6. 발언 키워드 분석
        List<KeywordDTO> keywords = getKeywords(figure.getId());

        // 7. 응답 데이터 생성
        return new DashboardResponse(
                FigureDTO.from(figure),
                billStats,
                statementStats,
                voteStats,
                keywords,
                billsOverTime
        );
    }

    /**
     * 법안 통계 조회
     * @param figureId
     * @return
     */
    private BillStatistics getBillStatistics(Long figureId) {
        List<Object[]> statusCounts = billRepository.countBillsByStatus(figureId);

        int total = 0;
        int passed = 0;
        int rejected = 0;
        int withdrawn = 0;
        int alternative = 0;
        int processing = 0;

        for (Object[] row : statusCounts) {
            BillStatus status = (BillStatus) row[0];
            int count = ((Number) row[1]).intValue();

            total += count;

            switch (status) {
                case PASSED -> passed = count;
                case REJECTED -> rejected = count;
                case WITHDRAWN -> withdrawn = count;
                case ALTERNATIVE -> alternative = count;
                case PROCESSING, PROPOSED, IN_COMMITTEE, IN_PLENARY -> processing += count;
            }
        }

        double passRate = total > 0 ? (double) passed / total * 100 : 0;

        return new BillStatistics(
                total,
                passed,
                rejected,
                withdrawn,
                alternative,
                processing,
                passRate);
    }

    /**
     * 발언 통계 조회
     * @param id
     * @return
     */
    private StatementStatistics getStatementStatistics(Long id) {
        return null;
    }

    /**
     * 투표 통계 조회
     * @param id
     * @return
     */
    private VoteStatistics getVoteStatistics(Long id) {
        return null;
    }

    /**
     * 월별 법안 발의 추이 조회
     * @param id
     * @return
     */
    private List<BillOverTimeDTO> getBillOverTime(Long id) {
        return null;
    }

    /**
     * 발언 키워드 분석
     * @param id
     * @return
     */
    private List<KeywordDTO> getKeywords(Long id) {
        return null;
    }
}

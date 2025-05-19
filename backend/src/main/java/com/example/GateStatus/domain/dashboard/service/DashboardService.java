package com.example.GateStatus.domain.dashboard.service;

import com.example.GateStatus.domain.dashboard.dto.internal.CategoryCount;
import com.example.GateStatus.domain.dashboard.dto.internal.KeywordCount;
import com.example.GateStatus.domain.dashboard.dto.response.*;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.VoteResultType;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * @return
     */
    private StatementStatistics getStatementStatistics(Long figureId) {
        long total = statementMongoRepository.countByFigureId(figureId);

        List<CategoryCount> categoryCounts = statementMongoRepository.countByCategory(figureId);

        Map<String, Integer> categoryDistribution = categoryCounts.stream()
                .collect(Collectors.toMap(
                        CategoryCount::id, // 맵의 키를 추출하는 함수
                        cat -> cat.count().intValue(), // 맵의 값을 추출하는 함수
                        (v1, v2) -> v1, // 키 충돌 시 값 병합 방법 (충돌 병합 함수)
                        LinkedHashMap::new)); // 결과 맵의 구현체 (맵 팩토리)

        String mostFrequentCategory = categoryCounts.isEmpty() ? "없음" : categoryCounts.get(0).id();

        return new StatementStatistics(
                (int) total,
                categoryDistribution,
                mostFrequentCategory
        );
    }

    /**
     * 투표 통계 조회
     * @return
     */
    private VoteStatistics getVoteStatistics(Long figureId) {
        List<Object[]> resultCounts = voteRepository.countVotesByResult(figureId);

        int agree = 0;
        int disagree = 0;
        int abstain = 0;
        int absent = 0;

        for (Object[] row : resultCounts) {
            VoteResultType result = (VoteResultType) row[0];
            int count = ((Number) row[1]).intValue();

            switch (result) {
                case AGREE -> agree = count;
                case DISAGREE -> disagree = count;
                case ABSTAIN -> abstain = count;
                case ABSENT -> absent = count;
            }
        }

        int total = agree + disagree + abstain + absent;
        double agreeRate = total > 0 ? (double) agree / total * 100 : 0;
        double participationRate = total > 0 ? (double) (agree + disagree + abstain) / total * 100 : 0;

        return new VoteStatistics(
                agree,
                disagree,
                abstain,
                absent,
                agreeRate,
                participationRate
        );
    }

    /**
     * 월별 법안 발의 추이 조회
     * @return
     */
    private List<BillOverTimeDTO> getBillOverTime(Long figureId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(2);

        List<Object[]> monthlyData = billRepository.countBillsByMonth(figureId, startDate, endDate);

        return monthlyData.stream()
                .map(row -> new BillOverTimeDTO(
                        (String) row[0],
                        ((Number) row[1]).intValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 발언 키워드 분석
     * @return
     */
    private List<KeywordDTO> getKeywords(Long figureId) {
        List<String> stopwords = List.of("이", "그", "저", "이것", "그것", "저것", "이런", "그런", "저런",
                "및", "등", "을", "를", "이다", "있다", "하다");

        List<KeywordCount> keywords = statementMongoRepository.findTopKeywords(figureId, stopwords);

        return keywords.stream()
                .map(kw -> new KeywordDTO(
                        kw.id(),
                        kw.count().intValue()
                ))
                .collect(Collectors.toList());
    }
}

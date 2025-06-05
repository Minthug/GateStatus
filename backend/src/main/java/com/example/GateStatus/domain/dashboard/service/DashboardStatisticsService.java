package com.example.GateStatus.domain.dashboard.service;

import com.example.GateStatus.domain.dashboard.dto.internal.CategoryCount;
import com.example.GateStatus.domain.dashboard.dto.internal.KeywordCount;
import com.example.GateStatus.domain.dashboard.dto.response.*;
import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.VoteResultType;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardStatisticsService {

    private final ProposedBillRepository billRepository;
    private final StatementMongoRepository statementMongoRepository;
    private final VoteRepository voteRepository;

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Cacheable(value = "keywords", key = "#figureId + '_' + #startDate + '_' + #endDate")
    public List<KeywordDTO> getKeywords(Long figureId, LocalDate startDate, LocalDate endDate) {
        List<String> stopwords = getStopwords();

        List<KeywordCount> keywords;

        if (startDate != null && endDate != null) {
            keywords = statementMongoRepository.findTopKeywordsByDateRange(figureId, stopwords, startDate, endDate);
        } else {
            keywords = statementMongoRepository.findTopKeywords(figureId, stopwords);
        }

        return keywords.stream()
                .map(kw -> new KeywordDTO(
                        kw.id(),
                        kw.count().intValue()
                ))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "billOverTime", key = "#figureId + '_' + #startDate + '_' + #endDate")
    public List<BillOverTimeDTO> getBillOverTime(Long figureId, LocalDate startDate, LocalDate endDate) {
        LocalDate actualStartDate = startDate != null ? startDate : LocalDate.now().minusYears(2);
        LocalDate actualEndDate = endDate != null ? endDate : LocalDate.now();

        List<Object[]> monthlyData = billRepository.countBillsByMonth(figureId, actualStartDate, actualEndDate);

        return monthlyData.stream()
                .map(row -> new BillOverTimeDTO(
                        (String) row[0],
                        ((Number) row[1]).intValue()
                ))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "voteStatistics", key = "#figureId + '_' + #startDate + '_' + #endDate")
    public VoteStatistics getVoteStatistics(Long figureId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> resultCounts;

        if (startDate != null && endDate != null) {
            resultCounts = voteRepository.countVotesByResultAndDateRange(figureId, startDate, endDate);
        } else {
            resultCounts = voteRepository.countVotesByResult(figureId);
        }

        VoteStatsCalculator calculator = new VoteStatsCalculator();

        for (Object[] row : resultCounts) {
            VoteResultType result = (VoteResultType) row[0];
            int count = ((Number) row[1]).intValue();
            calculator.addCount(result, count);
        }

        return calculator.build();
    }

    @Cacheable(value = "statementStatistics", key = "#figureId + '_' + #startDate + '_' + #endDate")
    public StatementStatistics getStatementStatistics(Long figureId, LocalDate startDate, LocalDate endDate) {
        long total;
        List<CategoryCount> categoryCounts;

        if (startDate != null && endDate != null) {
            total = statementMongoRepository.countByFigureIdAndDateRange(figureId, startDate, endDate);
            categoryCounts = statementMongoRepository.countByCategoryAndDateRange(figureId, startDate, endDate);
        } else {
            total = statementMongoRepository.countByFigureId(figureId);
            categoryCounts = statementMongoRepository.countByCategory(figureId);
        }

        Map<String, Integer> categoryDistribution = categoryCounts.stream()
                .collect(Collectors.toMap(
                        CategoryCount::id,
                        cat -> cat.count().intValue(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        String mostFrequentCategory = categoryCounts.isEmpty() ?
                "없음" : categoryCounts.get(0).id();

        return new StatementStatistics(
                (int) total,
                categoryDistribution,
                mostFrequentCategory);
    }

    @Cacheable(value = "billStatistics", key = "#figureId + '_' + #startDate + '_' + #endDate",
    unless = "#result.total == 0")
    public BillStatistics getBillStatistics(Long figureId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> statusCounts;

        if (startDate != null && endDate != null) {
            statusCounts = billRepository.countBillsByStatusAndDateRange(figureId, startDate, endDate);
        } else {
            statusCounts = billRepository.countBillsByStatus(figureId);
        }

        BillStatsCalculator calculator = new BillStatsCalculator();

        for (Object[] row : statusCounts) {
            BillStatus status = (BillStatus) row[0];
            int count = ((Number) row[1]).intValue();
            calculator.addCount(status, count);
        }
        return calculator.build();
    }


    // ===== Private Helper Methods =====

    /**
     * 불용어 목록 반환
     * 향후 외부 설정이나 DB에서 관리하도록 개선 가능
     *
     * @return 불용어 목록
     */
    private List<String> getStopwords() {
        return List.of("이", "그", "저", "이것", "그것", "저것", "이런", "그런", "저런",
                "및", "등", "을", "를", "이다", "있다", "하다", "그리고", "하지만",
                "그러나", "또한", "따라서", "그래서", "때문에");
    }

    // ===== Helper Classes =====

    public static class BillStatsCalculator {
        private int total = 0;
        private int passed = 0;
        private int rejected = 0;
        private int withdrawn = 0;
        private int alternative = 0;
        private int processing = 0;

    public void addCount(BillStatus status, int count) {
        total += count;

        switch (status) {
            case PASSED -> passed = count;
            case REJECTED -> rejected = count;
            case WITHDRAWN -> withdrawn = count;
            case ALTERNATIVE -> alternative = count;
            case PROCESSING, PROPOSED, IN_COMMITTEE, IN_PLENARY -> processing += count;
        }
    }

    public BillStatistics build() {
        double passRate = total > 0 ? (double) passed / total * 100 : 0;
        return new BillStatistics(total, passed, rejected, withdrawn, alternative, processing, passRate);
        }
    }

    public static class VoteStatsCalculator {
        private int agree = 0;
        private int disagree = 0;
        private int abstain = 0;
        private int absent = 0;

        public void addCount(VoteResultType result, int count) {
            switch (result) {
                case AGREE -> agree = count;
                case DISAGREE -> disagree = count;
                case ABSTAIN -> abstain = count;
                case ABSENT -> absent = count;
            }
        }

        public VoteStatistics build() {
            int total = agree + disagree + abstain + absent;
            double agreeRate = total > 0 ? (double) agree / total * 100 : 0;
            double participationRate = total > 0 ? (double) (agree + disagree + abstain) / total * 100 : 0;

            return new VoteStatistics(agree, disagree, abstain, absent, agreeRate, participationRate);
        }
    }
}

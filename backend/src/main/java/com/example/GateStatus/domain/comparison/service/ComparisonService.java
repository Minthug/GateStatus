package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.common.DateRange;
import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.comparison.service.response.CategoryInfo;
import com.example.GateStatus.domain.comparison.service.response.ComparisonResult;
import com.example.GateStatus.domain.comparison.service.response.IssueInfo;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.vote.Vote;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparisonService {

    private final ComparisonDataService dataService;
    private final ComparisonResultBuilder resultBuilder;
    private final PoliticalAnalysisService analysisService;
    private final FigureRepository figureRepository;
    private final IssueRepository issueRepository;


    /**
     * 정치인들을 이슈별로 비교하여 결과 반환
     * 전체 비교 프로세스를 조율하는 메인 메서드
     * @param request 비교 요청 정보
     * @return 비교 결과
     */
    @Transactional(readOnly = true)
    public ComparisonResult compareByIssue(ComparisonRequest request) {

        log.info("정치인 비교 요청 시작: figureIds={}, issueId={}",
                request.figureIds(), request.issueId());

        long startTime = System.currentTimeMillis();

        try {
            validateRequest(request);

            ComparisonContext context = createComparisonContext(request);
            DateRange dateRange = DateRange.fromRequest(request);

            ComparisonRawData rawData = dataService.fetchAllComparisonData(
                    request.figureIds(), context, dateRange);

            ComparisonResult result = resultBuilder.buildComparisonResult(
                    rawData, request.comparisonTypes());

            long endTime = System.currentTimeMillis();

            log.info("정치인 비교 완료: 처리시간 {}ms, 비교 대상 {}명",
                    endTime - startTime, request.figureIds().size());

            return result;
        } catch (Exception e) {
            log.error("정치인 비교 처리 중 오류 발생: figureIds={}, error={}",
                    request.figureIds(), e.getMessage(), e);

            throw e;
        }
    }

    private void validateRequest(ComparisonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("비교 요청이 null 입니다");
        }

        if (request.figureIds() == null || request.figureIds().isEmpty()) {
            throw new IllegalArgumentException("비교할 정치인을 최소 1명 이상 선택해야 합니다");
        }

        if (request.figureIds().size() > 10) {
            throw new IllegalArgumentException("비교 가능한 정치인은 최대 10명 이상입니다");
        }

        DateRange dateRange = DateRange.fromRequest(request);
        if (!dateRange.isValid()) {
            throw new IllegalArgumentException("유효하지 않은 날짜 범위 입니다: " + dateRange);
        }
    }

    @Builder
    @Value
    public static class ComparisonContext {
        IssueInfo issueInfo;
        CategoryInfo categoryInfo;

        public boolean hasTargetIssue() {
            return issueInfo != null;
        }

        public String getIssueId() {
            return issueInfo != null ? issueInfo.issueId() : null;
        }

        public boolean hasTargetCategory() {
            return categoryInfo != null;
        }
    }

    @Builder
    @Value
    public static class ComparisonRawData {
        Map<Long, Figure> figures;
        Map<Long, List<StatementDocument>> statements;
        Map<Long, List<Vote>> votes;
        Map<Long, List<ProposedBill>> bills;
        DateRange dateRange;
        ComparisonContext context;

        public boolean hasFigureData(Long figureId) {
            return figures.containsKey(figureId);
        }

        public String getDataSummary() {
            int totalStatements = statements.values().stream()
                    .mapToInt(List::size).sum();
            int totalVotes = votes.values().stream()
                    .mapToInt(List::size).sum();
            int totalBills = bills.values().stream()
                    .mapToInt(List::size).sum();

            return String.format("정치인 %d명, 발언 %d건, 투표 %d건, 법안 %d건",
                    figures.size(), totalStatements, totalVotes, totalBills);
        }
    }
}

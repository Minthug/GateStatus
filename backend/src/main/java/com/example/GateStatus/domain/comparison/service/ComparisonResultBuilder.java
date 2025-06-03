package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.common.DateRange;
import com.example.GateStatus.domain.comparison.ComparisonType;
import com.example.GateStatus.domain.comparison.service.response.*;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.vote.Vote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparisonResultBuilder {

    private final PoliticalAnalysisService analysisService;

    public ComparisonResult buildComparisonResult(ComparisonService.ComparisonRawData rawData,
                                                  List<ComparisonType> comparisonTypes) {
        log.debug("비교 결과 생성 시작: {}", rawData.getDataSummary());

        List<FigureComparisonData> figureDataList = createFigureComparisonDataList(rawData, comparisonTypes);

        Map<String, Object> summaryData = createSummaryData(rawData, figureDataList);

        ComparisonResult result = new ComparisonResult(
                figureDataList,
                rawData.getContext().getIssueInfo(),
                summaryData
        );

        log.debug("비교 결과 생성 완료: 정치인 {}명, 요약데이터 {}개 항목",
                figureDataList.size(), summaryData.size());

        return result;
    }

    private Map<String, Object> createSummaryData(ComparisonService.ComparisonRawData rawData, List<FigureComparisonData> figureDataList) {

        Map<String, Object> summary = new HashMap<>();

        addBasicAnalysisInfo(summary, rawData);

        addCategoryInfo(summary, rawData.getContext().getCategoryInfo());

        addTopPerformersInfo(summary, figureDataList);

        addOverallStatistics(summary, figureDataList);

        return summary;
    }

    private void addBasicAnalysisInfo(Map<String, Object> summary, ComparisonService.ComparisonRawData rawData) {
        DateRange dateRange = rawData.getDateRange();

        summary.put("analysisStartDate", dateRange.getStartDate().toString());
        summary.put("analysisEndDate", dateRange.getEndDate().toString());
        summary.put("analysisPeriodDays", dateRange.getDays());
        summary.put("analysisPeriodDescription", dateRange.getShortDescription());
        summary.put("comparedFiguresCount", rawData.getFigures().size());

        IssueInfo issueInfo = rawData.getContext().getIssueInfo();
        if (issueInfo != null) {
            summary.put("targetIssue", Map.of(
                    "id", issueInfo.issueId(),
                    "name", issueInfo.name(),
                    "description", issueInfo.description()
            ));
        }
    }

    private void addCategoryInfo(Map<String, Object> summary, CategoryInfo categoryInfo) {
        if (categoryInfo == null) return;

        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("code", categoryInfo.category().getCode());
        categoryData.put("name", categoryInfo.category().getDisplayName());
        categoryData.put("totalIssueCount", categoryInfo.issues().size());

        List<Map<String, Object>> topIssues = categoryInfo.issues().stream()
                .limit(5)
                .map(issue -> Map.of(
                        "id", issue.getId(),
                        "name", issue.getName(),
                        "viewCount", issue.getViewCount() != null ? issue.getViewCount() : 0
                ))
                .collect(Collectors.toList());

        categoryData.put("topIssues", topIssues);
        summary.put("categoryInfo", categoryData);
    }

    private void addTopPerformersInfo(Map<String, Object> summary, List<FigureComparisonData> figureDataList) {
        figureDataList.stream()
                .max(Comparator.comparingInt(f -> (Integer) f.additionalData().get("activeDays")))
                .ifPresent(figure -> summary.put("mostActiveFigure", createFigureInfo(figure)));

        figureDataList.stream()
                .filter(f -> f.statements() != null)
                .max(Comparator.comparingInt(f -> f.statements().statementCount()))
                .ifPresent(figure -> summary.put("mostActiveInStatements", createFigureInfo(figure)));

        figureDataList.stream()
                .filter(f -> f.votes() != null)
                .max(Comparator.comparingInt(f ->
                        f.votes().agreeCount() + f.votes().disagreeCount() + f.votes().abstainCount()))
                .ifPresent(figure -> summary.put("mostActiveInVoting", createFigureInfo(figure)));

        figureDataList.stream()
                .filter(f -> f.bills() != null)
                .max(Comparator.comparingInt(f -> f.bills().proposedCount()))
                .ifPresent(figure -> summary.put("mostActiveInBills", createFigureInfo(figure)));

    }


    private void addOverallStatistics(Map<String, Object> summary, List<FigureComparisonData> figureDataList) {
        int totalStatements = figureDataList.stream()
                .mapToInt(f -> f.statements() != null ? f.statements().statementCount() : 0)
                .sum();

        int totalVotes = figureDataList.stream()
                .mapToInt(f -> f.votes() != null ?
                        f.votes().agreeCount() + f.votes().disagreeCount() + f.votes().abstainCount() : 0)
                .sum();

        int totalBills = figureDataList.stream()
                .mapToInt(f -> f.bills() != null ? f.bills().proposedCount() : 0)
                .sum();

        summary.put("overallStatistics", Map.of(
                "totalStatements", totalStatements,
                "totalVotes", totalVotes,
                "totalBills", totalBills,
                "averageStatementsPerFigure", figureDataList.isEmpty() ? 0 : totalStatements / figureDataList.size(),
                "averageVotesPerFigure", figureDataList.isEmpty() ? 0 : totalVotes / figureDataList.size(),
                "averageBillsPerFigure", figureDataList.isEmpty() ? 0 : totalBills / figureDataList.size()
        ));

        Map<String, Long> partyDistribution = figureDataList.stream()
                .collect(Collectors.groupingBy(
                        FigureComparisonData::partyName,
                        Collectors.counting()
                ));
        summary.put("partyDistribution", partyDistribution);
    }

    private List<FigureComparisonData> createFigureComparisonDataList(ComparisonService.ComparisonRawData rawData, List<ComparisonType> comparisonTypes) {
        return null;
    }

    /**
     * 단일 정치인의 비교 데이터 생성
     */
    private FigureComparisonData createSingleFigureComparisonData(
            Figure figure,
            ComparisonService.ComparisonRawData rawData,
            List<ComparisonType> comparisonTypes) {

        Long figureId = figure.getId();

        List<StatementDocument> statements = rawData.getStatements().getOrDefault(figureId, Collections.emptyList());
        List<Vote> votes = rawData.getVotes().getOrDefault(figureId, Collections.emptyList());
        List<ProposedBill> bills = rawData.getBills().getOrDefault(figureId, Collections.emptyList());

        StatementComparisonData statementData = null;
        VoteComparisonData voteData = null;
        BillComparisonData billData = null;

        if (shouldIncludeType(comparisonTypes, ComparisonType.STATEMENT)) {
            statementData = createStatementComparisonData(statements);
        }

        if (shouldIncludeType(comparisonTypes, ComparisonType.VOTE)) {
            voteData = createVoteComparisonData(votes);
        }

        if (shouldIncludeType(comparisonTypes, ComparisonType.BILL)) {
            billData = createBillComparisonData(bills);
        }

        Map<String, Object> additionalData = createAdditionalData(
                figureId, statements, votes, bills, rawData.getDateRange());

        return new FigureComparisonData(
                figure.getId(),
                figure.getName(),
                figure.getFigureParty() != null ? figure.getFigureParty().getPartyName() : "무소속",
                statementData,
                voteData,
                billData,
                additionalData
        );

    }

    private Map<String, Object> createAdditionalData(Long figureId, List<StatementDocument> statements, List<Vote> votes, List<ProposedBill> bills, DateRange dateRange) {
        Map<String, Object> additionalData = new HashMap<>();

        Set<LocalDate> allActivityDates = new HashSet<>();

        statements.stream()
                .map(StatementDocument::getStatementDate)
                .forEach(allActivityDates::add);

        votes.stream()
                .map(Vote::getVoteDate)
                .forEach(allActivityDates::add);

        bills.stream()
                .map(ProposedBill::getProposeDate)
                .forEach(allActivityDates::add);

        int activeDays = allActivityDates.size();
        additionalData.put("activeDays", activeDays);
        
        double activeDensity = dateRange.calculateActivityDensity(activeDays);
        additionalData.put("activityDensity", Math.round(activeDensity * 10000) / 100.0);

        int totalActivities = statements.size() + votes.size() + bills.size();
        double monthlyAverage = dateRange.calculateMonthlyAverage(totalActivities);
        additionalData.put("monthlyAverageActivity", Math.round(monthlyAverage * 100) / 100.0);

        String mainActivityType = determineMainActivityType(
                statements.size(), votes.size(), bills.size());
        additionalData.put("mainActivityType", mainActivityType);

        Map<String, Integer> activityDistribution = Map.of(
                "statements", statements.size(),
                "votes", votes.size(),
                "bills", bills.size()
        );
        additionalData.put("activityDistribution", activityDistribution);
        return additionalData;
    }


    private VoteComparisonData createVoteComparisonData(List<Vote> votes) {
        if (votes.isEmpty()) {
            return new VoteComparisonData(Collections.emptyList(), 0, 0, 0, 0.0);
        }

        VoteResultStats stats = analysisService.calculateVoteStats(votes);

        List<VoteInfo> voteInfos = votes.stream()
                .limit(20)
                .map(this::convertToVoteInfo)
                .collect(Collectors.toList());

        return new VoteComparisonData(
                voteInfos,
                stats.agree(),
                stats.disagree(),
                stats.abstain(),
                stats.agreeRate()
        );
    }


    private BillComparisonData createBillComparisonData(List<ProposedBill> bills) {
        if (bills.isEmpty()) {
            return new BillComparisonData(Collections.emptyList(), 0, 0, 0.0);
        }

        BillPassStats stats = analysisService.calculateBillStats(bills);

        List<BillInfo> billInfos = bills.stream()
                .limit(20)
                .map(this::convertToBillInfo)
                .collect(Collectors.toList());

        return new BillComparisonData(
                billInfos,
                stats.total(),
                stats.passed(),
                stats.passRate()
        );
    }


    private StatementComparisonData createStatementComparisonData(List<StatementDocument> statements) {
        if (statements.isEmpty()) {
            return new StatementComparisonData(
                    Collections.emptyList(), 0, "입장 정보 없음", Collections.emptyMap());
        }

        List<StatementInfo> statementInfos = statements.stream()
                .limit(20)
                .map(this::convertToStatementInfo)
                .collect(Collectors.toList());

        Map<String, Integer> keywordCounts = analysisService.analyzeKeywordsFromStatements(statements, 10);
        String mainStance = analysisService.analyzeMainStance(statements);

        return new StatementComparisonData(
                statementInfos,
                statements.size(),
                mainStance,
                keywordCounts
        );
    }

    // === Helper Methods ===

    private String determineMainActivityType(int statementCount, int voteCount, int billCount) {
        if (statementCount >= voteCount && statementCount >= billCount) {
            return "발언 중심";
        } else if (voteCount >= billCount) {
            return "투표 참여 중심";
        } else {
            return "법안 발의 중심";
        }
    }

    private boolean shouldIncludeType(List<ComparisonType> types, ComparisonType targetType) {
        return types == null || types.contains(targetType);
    }


    private Map<String, Object> createFigureInfo(FigureComparisonData figure) {
        return Map.of(
                "id", figure.figureId(),
                "name", figure.figureName(),
                "party", figure.partyName()
        );
    }

    // === Entity to DTO 변환 메서드들 ===

    private StatementInfo convertToStatementInfo(StatementDocument document) {
        return new StatementInfo(
                document.getId(),
                document.getTitle(),
                analysisService.summarizeText(document.getContent(), 100),
                document.getStatementDate(),
                document.getSource()
        );
    }

    private VoteInfo convertToVoteInfo(Vote vote) {
        return new VoteInfo(
                vote.getId().toString(),
                vote.getBill() != null ? vote.getBill().getBillName() : "알 수 없음",
                vote.getVoteDate(),
                vote.getVoteResult().name()
        );
    }

    private BillInfo convertToBillInfo(ProposedBill bill) {
        return new BillInfo(
                bill.getId(),
                bill.getBillName(),
                bill.getProposeDate(),
                bill.getBillStatus() != null ? bill.getBillStatus().name() : "처리상태 알 수 없음",
                bill.getBillStatus() != null && bill.getBillStatus().isPassed()
        );
    }
}
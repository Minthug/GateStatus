package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.common.DateRange;
import com.example.GateStatus.domain.comparison.ComparisonType;
import com.example.GateStatus.domain.comparison.service.response.CategoryInfo;
import com.example.GateStatus.domain.comparison.service.response.ComparisonResult;
import com.example.GateStatus.domain.comparison.service.response.FigureComparisonData;
import com.example.GateStatus.domain.comparison.service.response.IssueInfo;
import com.example.GateStatus.domain.figure.Figure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Object createFigureInfo(FigureComparisonData figure) {
        return null;
    }
    /**
     * 단일 정치인의 비교 데이터 생성
     */
    private FigureComparisonData createSingleFigureComparisonData(
            Figure figure,
            ComparisonService.ComparisonRawData rawData,
            List<ComparisonType> comparisonTypes) {
        return ;
    }


}
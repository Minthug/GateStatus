package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.comparison.ComparisonType;
import com.example.GateStatus.domain.comparison.service.response.CategoryInfo;
import com.example.GateStatus.domain.comparison.service.response.ComparisonResult;
import com.example.GateStatus.domain.comparison.service.response.FigureComparisonData;
import com.example.GateStatus.domain.figure.Figure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    }

    private void addCategoryInfo(Map<String, Object> summary, CategoryInfo categoryInfo) {

    }

    private void addTopPerformersInfo(Map<String, Object> summary, List<FigureComparisonData> figureDataList) {

    }

    private void addOverallStatistics(Map<String, Object> summary, List<FigureComparisonData> figureDataList) {


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
        return ;
    }


}
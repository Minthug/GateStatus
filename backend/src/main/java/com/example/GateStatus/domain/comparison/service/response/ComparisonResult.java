package com.example.GateStatus.domain.comparison.service.response;

import java.util.List;
import java.util.Map;

public record ComparisonResult(
        List<FigureComparisonData> figures,
        IssueInfo issueInfo,
        Map<String, Object> summaryData
) {
}

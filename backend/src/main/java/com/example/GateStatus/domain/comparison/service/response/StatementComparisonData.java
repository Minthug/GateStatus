package com.example.GateStatus.domain.comparison.service.response;

import java.util.List;
import java.util.Map;

public record StatementComparisonData(
        List<StatementInfo> statements, // 관련 발언 목록
        int statementCount, // 총 발언 수
        String mainStance, // 주요 입장 (분석 결과)
        Map<String, Integer> keywordCounts // 주요 키워드 등장 횟수
) {
}

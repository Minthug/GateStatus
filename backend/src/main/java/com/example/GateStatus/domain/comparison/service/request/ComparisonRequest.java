package com.example.GateStatus.domain.comparison.service.request;

import com.example.GateStatus.domain.comparison.ComparisonType;

import java.time.LocalDate;
import java.util.List;

public record ComparisonRequest(
        List<Long> figureIds,
        List<String> figureNames,
        String issueId,
        String category,
        LocalDate startDate,
        LocalDate endDate,
        List<ComparisonType> comparisonTypes
) {
    /**
     * ID 기반 요청 생성 (기존 호환성)
     */
    public static ComparisonRequest byIds(List<Long> figureIds, String issueId, String category,
                                          LocalDate startDate, LocalDate endDate, List<ComparisonType> types) {
        return new ComparisonRequest(figureIds, null, issueId, category, startDate, endDate, types);
    }

    /**
     * 이름 기반 요청 생성 (새로운 기능)
     */
    public static ComparisonRequest byNames(List<String> figureNames, String issueId, String category,
                                            LocalDate startDate, LocalDate endDate, List<ComparisonType> types) {
        return new ComparisonRequest(null, figureNames, issueId, category, startDate, endDate, types);
    }

    /**
     * 혼합 요청 생성 (ID + 이름 모두)
     */
    public static ComparisonRequest mixed(List<Long> figureIds, List<String> figureNames, String issueId,
                                          String category, LocalDate startDate, LocalDate endDate, List<ComparisonType> types) {
        return new ComparisonRequest(figureIds, figureNames, issueId, category, startDate, endDate, types);
    }

    /**
     * 요청 타입 확인 메서드들
     */
    public boolean hasIds() {
        return figureIds != null && !figureIds.isEmpty();
    }

    public boolean hasNames() {
        return figureNames != null && !figureNames.isEmpty();
    }

    public boolean isMixed() {
        return hasIds() && hasNames();
    }

    /**
     * 유효성 검증
     */
    public boolean isValid() {
        return hasIds() || hasNames();
    }

    /**
     * 총 정치인 수 계산
     */
    public int getTotalFigureCount() {
        int count = 0;
        if (hasIds()) count += figureIds.size();
        if (hasNames()) count += figureNames.size();
        return count;
    }
}

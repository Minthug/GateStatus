package com.example.GateStatus.domain.news.dto;

import java.util.List;

public record TrendingKeyword(
        String keyword,
        Double score,
        Integer frequency,
        Double growthRate,
        List<String> recentNewsIds
) {
}

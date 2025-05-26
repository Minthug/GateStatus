package com.example.GateStatus.domain.news.dto;

public record TrendingKeyword(
        String keyword,
        Double trendScore,
        String content
) {
}

package com.example.GateStatus.domain.news.dto;

import java.util.List;

public record KeywordStats(
        String keyword,
        Long frequency,
        List<String> relatedCategories,
        List<String> newsSource
) {
}

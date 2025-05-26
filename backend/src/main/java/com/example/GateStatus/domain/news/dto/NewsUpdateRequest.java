package com.example.GateStatus.domain.news.dto;

import java.util.List;

public record NewsUpdateRequest(
        String category,
        List<String> extractedKeywords,
        List<Long> mentionedFigureIds
) {
}

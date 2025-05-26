package com.example.GateStatus.domain.news.dto;

import java.util.List;

public record FigureMentionStats(
        Long figureId,
        Long count,
        List<String> mentionedInCategories
) {
}

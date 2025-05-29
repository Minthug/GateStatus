package com.example.GateStatus.domain.issue.service.response;

public record IssueSuggestion(
        String id,
        String name,
        String categoryName,
        int matchScore
) {
}

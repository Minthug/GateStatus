package com.example.GateStatus.domain.comparison.service.response;

public record IssueInfo(
        String issueId,
        String name,
        String description,
        String categoryName
) {
}

package com.example.GateStatus.domain.issue.service.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record SearchResponse(
        List<IssueSuggestion> suggestions,
        Page<IssueResponse> results
) {
}

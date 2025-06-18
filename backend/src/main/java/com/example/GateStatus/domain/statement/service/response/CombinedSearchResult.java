package com.example.GateStatus.domain.statement.service.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record CombinedSearchResult(
        Page<StatementResponse> databaseResults,
        List<StatementResponse> apiResults
) {
}

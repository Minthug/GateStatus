package com.example.GateStatus.domain.issue.service.request;

import java.util.List;

public record IssueRequest(String name,
                           String description,
                           String categoryCode,
                           List<String> keywords,
                           String thumbnailUrl,
                           String parentIssueId,
                           Boolean isActive,
                           Integer priority,
                           Boolean isHot,
                           List<String> tags,
                           List<String> relatedStatementIds,
                           List<String> relatedBillIds,
                           List<Long> relatedFigureIds,
                           List<String> relatedNewsIds) {
}

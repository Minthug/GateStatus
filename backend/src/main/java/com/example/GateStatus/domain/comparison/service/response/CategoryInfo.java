package com.example.GateStatus.domain.comparison.service.response;

import com.example.GateStatus.domain.issue.IssueCategory;
import com.example.GateStatus.domain.issue.IssueDocument;

import java.util.List;

public record CategoryInfo(
        IssueCategory category,
        List<IssueDocument> issues
) {
}

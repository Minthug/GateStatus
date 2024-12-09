package com.example.GateStatus.domain.issue.repository.request;

import com.example.GateStatus.domain.issue.repository.response.FindIssueDetailResponse;

public record FindIssueDetailCommand(Long issueId) {

    public static FindIssueDetailCommand from(final Long issueId) {
        return new FindIssueDetailCommand(issueId);
    }
}

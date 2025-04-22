package com.example.GateStatus.domain.issue.service.request;

public record FindIssueDetailCommand(Long issueId) {

    public static FindIssueDetailCommand from(final Long issueId) {
        return new FindIssueDetailCommand(issueId);
    }
}

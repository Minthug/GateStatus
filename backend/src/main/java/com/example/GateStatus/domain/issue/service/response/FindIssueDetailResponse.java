package com.example.GateStatus.domain.issue.service.response;

public record FindIssueDetailResponse(IssueDetailResponse issue) {

    public static FindIssueDetailResponse of(final IssueDetailResponse issue) {
        return new FindIssueDetailResponse(issue);
    }

    public record IssueDetailResponse(Long issueId, String title, String content) {
    }
}

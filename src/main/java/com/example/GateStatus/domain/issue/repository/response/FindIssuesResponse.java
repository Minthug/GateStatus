package com.example.GateStatus.domain.issue.repository.response;

import com.example.GateStatus.domain.issue.Issue;

public record FindIssuesResponse() {

    public static FindIssuesResponse of(final ) {
    }

    public record FindIssueResponse(Long issueId, String title, String content) {

        public static FindIssueResponse from(Issue issue) {
            return new FindIssueResponse(
                    issue.getId(),
                    issue.getTitle(),
                    issue.getContent());
        }
    }
}

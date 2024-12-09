package com.example.GateStatus.domain.issue.repository.response;

import com.example.GateStatus.domain.issue.Issue;

import java.util.List;
import java.util.stream.Collectors;

public record FindIssuesResponse(List<FindIssueResponse> issues) {

    public static FindIssuesResponse of(final List<FindIssueResponse> issues) {
        return new FindIssuesResponse(issues);
    }

    public static FindIssuesResponse fromRedis(List<IssueRedisDto> issues) {
        return new FindIssuesResponse(
                issues.stream()
                        .map(FindIssueResponse::from)
                        .collect(Collectors.toList())
        );
    }

    public record FindIssueResponse(Long issueId, String title, String content) {

        public static FindIssueResponse from(Issue issue) {
            return new FindIssueResponse(
                    issue.getId(),
                    issue.getTitle(),
                    issue.getContent());
        }

        public static FindIssueResponse from(IssueRedisDto issueRedisDto) {
            return new FindIssueResponse(
                    issueRedisDto.issueId(),
                    issueRedisDto.title(),
                    issueRedisDto.content()
            );
        }
    }
}

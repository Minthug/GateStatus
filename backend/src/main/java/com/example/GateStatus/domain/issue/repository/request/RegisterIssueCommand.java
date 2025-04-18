package com.example.GateStatus.domain.issue.repository.request;

public record RegisterIssueCommand(String title, String content) {

    public static RegisterIssueCommand of(final String title, final String content) {
        return new RegisterIssueCommand(title, content);
    }
}

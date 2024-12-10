package com.example.GateStatus.domain.issue.repository.response;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.repository.response.FindIssuesResponse.FindIssueResponse;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.Collections;

public record IssueRedisDto(Long issueId, String title, String content,
                            @JsonSerialize(using = LocalDateTimeSerializer.class)
                            @JsonDeserialize(using = LocalDateTimeDeserializer.class)
                            LocalDateTime createdAt) {

    public static IssueRedisDto from(final Issue issue) {
        return new IssueRedisDto(
                issue.getId(),
                issue.getTitle(),
                issue.getContent(),
                issue.getCreatedAt()
        );
    }

    public static IssueRedisDto from(FindIssueResponse response,
                                     LocalDateTime createdAt) {
        return new IssueRedisDto(
                response.issueId(),
                response.title(),
                response.content(),
                createdAt,
        );
    }
}

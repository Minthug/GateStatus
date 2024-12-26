package com.example.GateStatus.domain.issue.repository.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterIssueRequest(@NotBlank(message = "이슈 제목은 필수 입력 사항입니다")
                                   String title,
                                   @NotBlank(message = "이슈 내용은 필수 입력 사항입니다")
                                   String content) {
}

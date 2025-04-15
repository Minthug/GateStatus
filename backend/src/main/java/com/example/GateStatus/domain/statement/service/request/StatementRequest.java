package com.example.GateStatus.domain.statement.service.request;

import com.example.GateStatus.domain.statement.StatementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StatementRequest(@NotNull(message = "발언자 ID는 필수 입니다") Long figureId,
                               @NotBlank(message = "제목은 필수 입니다") String title,
                               @NotBlank(message = "내용은 필수 입니다") String content,
                               @NotNull(message = "발언 날짜는 필수 입니다") @PastOrPresent(message = "발언 날짜는 현재 또는 과거여야 합니다") LocalDate statementDate,
                               @NotBlank(message = "출처는 필수 입니다") String source,
                               String context,
                               @NotBlank(message = "원본 URL은 필수 입니다") String originalUrl,
                               StatementType type) {
}
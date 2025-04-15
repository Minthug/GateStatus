package com.example.GateStatus.domain.statement.service.response;

import com.example.GateStatus.domain.statement.Statement;
import com.example.GateStatus.domain.statement.StatementType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StatementResponse(Long id,
                                Long figureId,
                                String figureName,
                                String title,
                                String content,
                                LocalDate statementDate,
                                String source,
                                String context,
                                String originalUrl,
                                StatementType type,
                                Integer factCheckScore,
                                String factCheckResult,
                                Integer viewCount,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {

    public static StatementResponse from(Statement statement) {
        return new StatementResponse(
                statement.getId(),
                statement.getFigure().getId(),
                statement.getFigure().getName(),
                statement.getTitle(),
                statement.getContent(),
                statement.getStatementDate(),
                statement.getSource(),
                statement.getContext(),
                statement.getOriginalUrl(),
                statement.getType(),
                statement.getFactCheckScore(),
                statement.getFactCheckResult(),
                statement.getViewCount(),
                statement.getCreatedAt(),
                statement.getUpdatedAt()
        );
    }
}

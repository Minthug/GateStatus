package com.example.GateStatus.domain.statement.service.response;

import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StatementResponse(String id,
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

    public static StatementResponse from(StatementDocument statement) {
        return new StatementResponse(
                statement.getId(),
                statement.getFigureId(),
                statement.getFigureName(),
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

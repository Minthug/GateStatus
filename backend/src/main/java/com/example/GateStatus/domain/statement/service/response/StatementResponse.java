package com.example.GateStatus.domain.statement.service.response;

import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
                                List<String> checkableItems,
                                Map<String, Object> nlpData,
                                Integer viewCount,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MongoDB Document -> DTO
     */
    public static StatementResponse from(StatementDocument statement) {

        List<String> checkableItems = null;
        if (statement.getNlpData() != null && statement.getNlpData().containsKey("checkableItems")) {
            checkableItems = (List<String>) statement.getNlpData().get("checkableItems");
        }

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
                checkableItems,
                statement.getNlpData(),
                statement.getViewCount(),
                statement.getCreatedAt(),
                statement.getUpdatedAt()
        );
    }

    /**
     * Jpa Entity -> DTO
     */
    public static StatementResponse from(Statement entity) {
        return new StatementResponse(
                entity.getId().toString(),
                entity.getFigure().getId(),
                entity.getFigure().getName(),
                entity.getTitle(),
                entity.getContent(),
                entity.getStatementDate(),
                entity.getSource(),
                entity.getContext(),
                entity.getOriginalUrl(),
                entity.getType(),
                entity.getFactCheckScore(),
                entity.getFactCheckResult(),
                null,  // JPA Entity에는 checkableItems 없음
                null,  // JPA Entity에는 nlpData 없음
                entity.getViewCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

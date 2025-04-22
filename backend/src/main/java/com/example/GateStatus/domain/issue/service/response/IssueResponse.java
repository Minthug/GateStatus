package com.example.GateStatus.domain.issue.service.response;

import com.example.GateStatus.domain.issue.IssueDocument;

import java.time.LocalDateTime;
import java.util.List;

public record IssueResponse(String id,
                            String name,
                            String description,
                            String categoryCode,
                            String categoryName,
                            List<String> keywords,
                            String thumbnailUrl,
                            String parentIssueId,
                            Boolean isActive,
                            Integer priority,
                            Integer viewCount,
                            Boolean isHot,
                            List<String> relatedStatementIds,
                            List<String> relatedBillIds,
                            List<Long> relatedFigureIds,
                            List<String> tags,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {

    public static IssueResponse from(IssueDocument document) {
        return new IssueResponse(
                document.getId(),
                document.getName(),
                document.getDescription(),
                document.getCategoryCode(),
                document.getCategoryName(),
                document.getKeywords(),
                document.getThumbnailUrl(),
                document.getParentIssueId(),
                document.getIsActive(),
                document.getPriority(),
                document.getViewCount(),
                document.getIsHot(),
                document.getRelatedStatementIds(),
                document.getRelatedBillIds(),
                document.getRelatedFigureIds(),
                document.getTags(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}

package com.example.GateStatus.domain.news.dto;

import com.example.GateStatus.domain.news.NewsDocument;

import java.time.LocalDateTime;
import java.util.List;

public record NewsResponse(
        String id,
        String title,
        String description,
        String link,
        String originalLink,
        LocalDateTime pubDate,
        String source,
        String category,
        Integer viewCount,
        Integer commentCount,
        List<String> keywords,
        String relatedIssueId,
        Boolean processed,
        LocalDateTime createdAt
) {

    public static NewsResponse from(NewsDocument doc) {
        return new NewsResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getLink(),
                doc.getOriginalLink(),
                doc.getPubDate(),
                doc.getSource(),
                doc.getCategory(),
                doc.getViewCount(),
                doc.getCommentCount(),
                doc.getExtractedKeywords(),
                doc.getRelatedIssueId(),
                doc.getProcessed(),
                doc.getCreatedAt()
        );
    }

}

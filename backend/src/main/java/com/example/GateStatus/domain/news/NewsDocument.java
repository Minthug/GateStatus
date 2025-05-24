package com.example.GateStatus.domain.news;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "news")
@Data
@Builder
public class NewsDocument {

    @Id
    private String id;

    private String title;
    private String description;
    private String link;
    private String originalLink;
    private LocalDateTime pubDate;
    private String source;

    @Indexed
    private boolean processed = false;

    private Integer viewCount;
    private Integer commentCount;

    private List<String> extractedKeywords;
    private String category;

    private String relatedIssueId;
    private List<Long> mentionedFigureIds;

    @CreatedDate
    private LocalDateTime createdAt;
}

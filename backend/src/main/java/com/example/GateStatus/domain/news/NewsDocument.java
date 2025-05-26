package com.example.GateStatus.domain.news;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "news")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "source_date_idx", def = "{'source': 1, 'pubDate': -1}")
@CompoundIndex(name = "processed_date_idx", def = "{'processed': 1, 'pubDate': -1}")
public class NewsDocument {

    @Id
    private String id;

    @TextIndexed(weight = 2)
    private String title;
    private String description;
    private String link;
    private String originalLink;
    @TextIndexed
    private LocalDateTime pubDate;
    @TextIndexed
    private String source;

    @Indexed
    private boolean processed = false;

    private Integer viewCount;
    private Integer commentCount;

    @Builder.Default
    private List<String> extractedKeywords;

    @Indexed
    private String category;

    @Indexed
    private String relatedIssueId;

    @Builder.Default
    private List<Long> mentionedFigureIds;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime processedAt;

    private String contentHash;
}

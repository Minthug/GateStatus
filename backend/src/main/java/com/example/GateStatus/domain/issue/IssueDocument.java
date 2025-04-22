package com.example.GateStatus.domain.issue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "issues")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndex(name = "category_active_idx", def = "{'categoryCode': 1, 'isActive': 1}")
public class IssueDocument {

    @Id
    private Long id;

    @TextIndexed(weight = 2)
    private String name;

    @TextIndexed
    private String description;

    @Indexed
    private String categoryCode;

    private String categoryName;

    @TextIndexed
    private List<String> keywords;

    private String thumbnailUrl;

    private String parentIssueId;

    @Indexed
    private Boolean isActive;

    private Integer priority;

    private Integer viewCount;

    private Boolean isHot;

    @Builder.Default
    private List<String> relatedStatementIds = new ArrayList<>();

    @Builder.Default
    private List<String> relatedBillIds = new ArrayList<>();

    @Builder.Default
    private List<Long> relatedFigureIds = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void incrementViewCount() {
        if (viewCount == null) {
            this.viewCount = 1;
        } else {
            this.viewCount++;
        }
    }

    public void addRelatedStatement(String statementId) {
        if (this.relatedStatementIds == null) {
            this.relatedStatementIds = new ArrayList<>();
        }

        if (!this.relatedStatementIds.contains(statementId)) {
            this.relatedStatementIds.add(statementId);
        }
    }

    public void addRelatedBill(String billId) {
        if (this.relatedBillIds == null) {
            this.relatedBillIds = new ArrayList<>();
        }

        if (!relatedBillIds.contains(billId)) {
            this.relatedBillIds.add(billId);
        }
    }

    public void addRelatedFigure(Long figureId) {
        if (this.relatedFigureIds == null) {
            this.relatedFigureIds = new ArrayList<>();
        }

        if (!relatedFigureIds.contains(figureId)) {
            this.relatedFigureIds.add(figureId);
        }
    }

    public void update(String name, String description, String categoryCode, String categoryName,
                       List<String> keywords, String thumbnailUrl, List<String> tags, Boolean isActive, Boolean isHot) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (categoryCode != null) this.categoryCode = categoryCode;
        if (categoryName != null) this.categoryName = categoryName;
        if (keywords != null) this.keywords = keywords;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (tags != null) this.tags = tags;
        if (isActive != null) this.isActive = isActive;
        if (isHot != null) this.isHot = isHot;
    }
}

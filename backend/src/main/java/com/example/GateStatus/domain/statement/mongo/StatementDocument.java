package com.example.GateStatus.domain.statement.mongo;

import com.example.GateStatus.domain.statement.entity.StatementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "figure_data_idx", def = "{'figureId': 1, 'statementDate': -1}")
public class StatementDocument {

    @Id
    private String id;

    @Indexed
    private Long figureId;

    private String figureName;

    @TextIndexed(weight = 2)
    private String title;

    @TextIndexed(weight = 1)
    private String content;

    private LocalDate statementDate;

    private String source;
    private String context;

    @Indexed(unique = true)
    private String originalUrl;

    @Indexed
    private StatementType type;

    private Integer factCheckScore;
    private String factCheckResult;

    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> issueIds;

    public void incrementViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        this.viewCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFactCheck(Integer score, String result) {
        this.factCheckScore = score;
        this.factCheckResult = result;
        this.updatedAt = LocalDateTime.now();
    }
}

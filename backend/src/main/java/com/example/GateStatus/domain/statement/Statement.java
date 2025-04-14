package com.example.GateStatus.domain.statement;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "statements")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Statement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "figure_id", nullable = false)
    private Figure figure;

    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDate statementDate;

    private String source;

    @Column(columnDefinition = "TEXT")
    private String context;

    private String originalUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementType type;

    private Integer factCheckSource;

    private String factCheckResult;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.viewCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateFactCheck(Integer score, String result) {
        this.factCheckSource = score;
        this.factCheckResult = result;
    }
}

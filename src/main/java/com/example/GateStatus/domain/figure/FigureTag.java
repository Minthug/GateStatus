package com.example.GateStatus.domain.figure;

import com.example.GateStatus.domain.tag.Tag;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class FigureTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "figure_id")
    private Figure figure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    private LocalDateTime taggedAt;
    private String taggedBy;

    public FigureTag(Figure figure, Tag tag) {
        this.figure = figure;
        this.tag = tag;
        this.taggedAt = LocalDateTime.now();
    }

    public static FigureTag createFigureTag(Figure figure, Tag tag, String taggedBy) {
        FigureTag figureTag = new FigureTag(figure, tag);
        figureTag.taggedBy = taggedBy;
        return figureTag;
    }
}

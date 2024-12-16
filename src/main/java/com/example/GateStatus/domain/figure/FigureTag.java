package com.example.GateStatus.domain.figure;

import com.example.GateStatus.domain.tag.Tag;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
public class FigureTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Figure figure;

    @ManyToOne(fetch = FetchType.LAZY)
    private Tag tag;

    private String taggedBy;
}

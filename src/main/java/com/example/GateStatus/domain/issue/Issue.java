package com.example.GateStatus.domain.issue;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Issue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Figure figure;

    private String title;
    private String content;
    private String thumbnailUrl;
    private String ipAddress;

    @ElementCollection
    private List<String> tags;

    private int viewCount;
    private boolean isHot;

}

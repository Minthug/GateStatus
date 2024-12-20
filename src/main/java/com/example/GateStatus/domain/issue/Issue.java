package com.example.GateStatus.domain.issue;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public Issue(String title, String content) {
        this.title = title;
        this.content = content;
    }
}

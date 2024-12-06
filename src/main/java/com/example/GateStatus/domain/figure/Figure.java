package com.example.GateStatus.domain.figure;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Figure extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    private FigureType figureType;

    @OneToMany(mappedBy = "figure")
    private List<Issue> issues;

}

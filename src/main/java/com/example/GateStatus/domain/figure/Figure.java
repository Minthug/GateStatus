package com.example.GateStatus.domain.figure;

import com.example.GateStatus.domain.career.Career;
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
    private String englishName;
    private String birth; // 출생일
    private String place; // 활동지
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    private FigureType figureType;

    @ElementCollection
    private List<String> education;

    @ElementCollection
    private List<Career> careers;

    @ElementCollection
    private List<String> sites;

    @ElementCollection
    private List<String> activities;

    private String updateSource;
}

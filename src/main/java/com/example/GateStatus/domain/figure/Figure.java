package com.example.GateStatus.domain.figure;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.tag.Tag;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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

    @Enumerated(EnumType.STRING)
    private FigureParty figureParty;

    @ElementCollection
    private List<String> education;

    @ElementCollection
    private List<Career> careers;

    @ElementCollection
    private List<String> sites;

    @ElementCollection
    private List<String> activities;

    private String updateSource;

    @OneToMany(mappedBy = "figure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FigureTag> figureTag = new ArrayList<>();

    public void update(
            String name,
            String englishName,
            String birth,
            String place,
            String profileUrl,
            FigureType figureType,
            FigureParty figureParty,
            List<String> education,
            List<Career> careers,
            List<String> sites,
            List<String> activities,
            String updateSource) {
        this.name = name;
        this.englishName = englishName;
        this.birth = birth;
        this.place = place;
        this.profileUrl = profileUrl;
        this.figureType = figureType;
        this.figureParty = figureParty;
        this.education = education;
        this.careers = careers;
        this.sites = sites;
        this.activities = activities;
        this.updateSource = updateSource;
    }

    public void addFigureTag(Tag tag) {
        FigureTag figureTag = new FigureTag(this, tag);
        this.figureTag.add(figureTag);
    }

    public void removeFigureTag(Tag tag) {
        this.figureTag.remove(tag);
        tag.getFigureTags().remove(this);
    }
}

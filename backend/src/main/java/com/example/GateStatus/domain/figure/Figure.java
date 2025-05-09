package com.example.GateStatus.domain.figure;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.category.Category;
import com.example.GateStatus.domain.tag.Tag;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Figure extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "figure_id", unique = true, nullable = false)
    private String figureId;
    private String name;
    private String englishName;
    private String birth; // 출생일
    private String constituency; // 활동지
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    private FigureType figureType;

    @Enumerated(EnumType.STRING)
    private FigureParty figureParty;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "figure_education",
            joinColumns = @JoinColumn(name = "figure_id")
    )
    @Column(name = "education")
    private List<String> education;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "figure_career",
            joinColumns = @JoinColumn(name = "figure_id")
    )
    private List<Career> careers;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "figure_site",
            joinColumns = @JoinColumn(name = "figure_id")
    )
    @Column(name = "site")
    private List<String> sites;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "figure_activity",
            joinColumns = @JoinColumn(name = "figure_id")
    )
    @Column(name = "activity")
    private List<String> activities;

    private String updateSource;

    private Long viewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ElementCollection
    private List<String> issueIds;

    @OneToMany(mappedBy = "figure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FigureTag> figureTag = new ArrayList<>();

    public void update(
            String name,
            String englishName,
            String birth,
            String constituency,
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
        this.constituency = constituency;
        this.profileUrl = profileUrl;
        this.figureType = figureType;
        this.figureParty = figureParty;
        this.education = education;
        this.careers = careers;
        this.sites = sites;
        this.activities = activities;
        this.updateSource = updateSource;
    }

    public FigureTag addFigureTag(Tag tag) {
        FigureTag figureTag = new FigureTag(this, tag);
        this.figureTag.add(figureTag);
        return figureTag;
    }

    public void removeFigureTag(Tag tag) {
        this.figureTag.remove(tag);
        tag.getFigureTags().remove(this);
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void clearAllCollections() {
        if (this.education != null) this.education.clear();
        if (this.careers != null) this.careers.clear();
        if (this.sites != null) this.sites.clear();
        if (this.activities != null) this.activities.clear();
    }
}

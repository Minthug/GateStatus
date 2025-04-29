package com.example.GateStatus.domain.figure.service.response;

import com.example.GateStatus.domain.career.CareerDTO;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class FigureDTO {
    private String figureId;
    private String name;
    private String englishName;
    private String birth;
    private String constituency;
    private String profileUrl;
    private FigureType figureType;
    private FigureParty figureParty;
    private List<String> education;
    private List<CareerDTO> careers;
    private List<String> sites;
    private List<String> activities;
    private Long viewCount;
    private String categoryName;

    // 필요한 필드만 포함

    public static FigureDTO from(Figure figure) {
        return FigureDTO.builder()
                .figureId(figure.getFigureId())
                .name(figure.getName())
                .englishName(figure.getEnglishName())
                .birth(figure.getBirth())
                .constituency(figure.getConstituency())
                .profileUrl(figure.getProfileUrl())
                .figureType(figure.getFigureType())
                .figureParty(figure.getFigureParty())
                .education(figure.getEducation() != null ? new ArrayList<>(figure.getEducation()) : new ArrayList<>())
                .careers(figure.getCareers() != null ?
                        figure.getCareers().stream().map(CareerDTO::from).collect(Collectors.toList()) :
                        new ArrayList<>())
                .sites(figure.getSites() != null ? new ArrayList<>(figure.getSites()) : new ArrayList<>())
                .activities(figure.getActivities() != null ? new ArrayList<>(figure.getActivities()) : new ArrayList<>())
                .viewCount(figure.getViewCount())
                .categoryName(figure.getCategory() != null ? figure.getCategory().getName() : null)
                .build();
    }
}

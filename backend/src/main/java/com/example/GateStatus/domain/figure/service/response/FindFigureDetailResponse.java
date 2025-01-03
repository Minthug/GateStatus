package com.example.GateStatus.domain.figure.service.response;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;

import java.util.List;

public record FindFigureDetailResponse(Long figureId,
                                       String name,
                                       String englishName,
                                       String birth,
                                       String place,
                                       String profileUrl,
                                       FigureType figureType,
                                       List<String> education,
                                       List<Career> careers,
                                       List<String> sites,
                                       List<String> activities,
                                       String updateSource) {
    public static FindFigureDetailResponse from(final Figure findFigure) {
        return new FindFigureDetailResponse(
                findFigure.getId(),
                findFigure.getName(),
                findFigure.getEnglishName(),
                findFigure.getBirth(),
                findFigure.getPlace(),
                findFigure.getProfileUrl(),
                findFigure.getFigureType(),
                findFigure.getEducation(),
                findFigure.getCareers(),
                findFigure.getSites(),
                findFigure.getActivities(),
                findFigure.getUpdateSource());
    }
}

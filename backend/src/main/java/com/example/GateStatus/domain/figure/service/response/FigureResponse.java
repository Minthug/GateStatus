package com.example.GateStatus.domain.figure.service.response;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;

import java.util.List;

public record FigureResponse(
        Long id,
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
        String updateSource
) {
    public static FigureResponse from(Figure figure) {
        return new FigureResponse(
                figure.getId(),
                figure.getName(),
                figure.getEnglishName(),
                figure.getBirth(),
                figure.getPlace(),
                figure.getProfileUrl(),
                figure.getFigureType(),
                figure.getFigureParty(),
                figure.getEducation(),
                figure.getCareers(),
                figure.getSites(),
                figure.getActivities(),
                figure.getUpdateSource()
        );

    }
}

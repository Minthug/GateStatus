package com.example.GateStatus.domain.figure.service.response;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import lombok.Builder;

import java.util.List;

public record UpdateFigureResponse(
        Long id,
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
        String updateSource
) {
    public static UpdateFigureResponse from(Figure figure) {
        return new UpdateFigureResponse(
                figure.getId(),
                figure.getName(),
                figure.getEnglishName(),
                figure.getBirth(),
                figure.getPlace(),
                figure.getProfileUrl(),
                figure.getFigureType(),
                figure.getEducation(),
                figure.getCareers(),
                figure.getSites(),
                figure.getActivities(),
                figure.getUpdateSource()
        );
    }
}
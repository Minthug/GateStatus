package com.example.GateStatus.domain.figure.service.request;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import lombok.Builder;

import java.util.List;

public record UpdateFigureCommand(
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
}

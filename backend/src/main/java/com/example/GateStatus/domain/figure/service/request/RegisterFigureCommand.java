package com.example.GateStatus.domain.figure.service.request;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

public record RegisterFigureCommand(String name,
                                    String englishName,
                                    String birth,
                                    String constituency,
                                    String profileUrl,
                                    FigureType figureType,
                                    List<String> education,
                                    List<Career> careers,
                                    List<String> sites,
                                    List<String> activities,
                                    String updateSource) {

    public static RegisterFigureCommand of(final String name,
                                           final String englishName,
                                           final String birth,
                                           final String constituency,
                                           final String profileUrl,
                                           final FigureType figureType,
                                           final List<String> education,
                                           final List<Career> careers,
                                           final List<String> sites,
                                           final List<String> activities,
                                           final String updateSource) {
        return new RegisterFigureCommand(
                name,
                englishName,
                birth,
                constituency,
                profileUrl,
                figureType,
                education != null ? education : new ArrayList<>(),
                careers != null ? careers : new ArrayList<>(),
                sites != null ? sites : new ArrayList<>(),
                activities != null ? activities : new ArrayList<>(),
                updateSource);
    }
}

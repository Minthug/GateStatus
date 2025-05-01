package com.example.GateStatus.domain.figure.service.request;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record UpdateFigureRequest(
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
        String updateSource
) {

    public static UpdateFigureRequest from(Figure figure) {
        return new UpdateFigureRequest(
                figure.getName(),
                figure.getEnglishName(),
                figure.getBirth(),
                figure.getConstituency(),
                figure.getProfileUrl(),
                figure.getFigureType(),
                figure.getFigureParty(),
                figure.getEducation() != null ? new ArrayList<>(figure.getEducation()) : new ArrayList<>(),
                figure.getCareers() != null ? new ArrayList<>(figure.getCareers()) : new ArrayList<>(),
                figure.getSites() != null ? new ArrayList<>(figure.getSites()) : new ArrayList<>(),
                figure.getActivities() != null ? new ArrayList<>(figure.getActivities()) : new ArrayList<>(),
                figure.getUpdateSource()
        );
    }

    /**
     * FigureDTO로부터 요청 객체 생성
     * 기존 국회의원 정보를 기반으로 요청 객체를 생성할 때 사용
     */
    public static UpdateFigureRequest fromDto(FigureDTO dto) {
        List<Career> careerList = null;
        if (dto.getCareers() != null) {
            careerList = dto.getCareers().stream()
                    .map(careerDTO -> Career.builder()
                            .title(careerDTO.getTitle())
                            .position(careerDTO.getPosition())
                            .organization(careerDTO.getOrganization())
                            .period(careerDTO.getPeriod())
                            .build())
                    .collect(Collectors.toList());
        } else {
            careerList = new ArrayList<>();
        }

        return new UpdateFigureRequest(
                dto.getName(),
                dto.getEnglishName(),
                dto.getBirth(),
                dto.getConstituency(),
                dto.getProfileUrl(),
                dto.getFigureType(),
                dto.getFigureParty(),
                dto.getEducation() != null ? new ArrayList<>(dto.getEducation()) : new ArrayList<>(),
                careerList,
                dto.getSites() != null ? new ArrayList<>(dto.getSites()) : new ArrayList<>(),
                dto.getActivities() != null ? new ArrayList<>(dto.getActivities()) : new ArrayList<>(),
                null // DTO에는 updateSource가 없으므로 null로 설정
        );
    }
}

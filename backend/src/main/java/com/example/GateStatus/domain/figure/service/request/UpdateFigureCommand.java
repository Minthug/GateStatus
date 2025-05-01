package com.example.GateStatus.domain.figure.service.request;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;

import java.util.List;
import java.util.stream.Collectors;

public record UpdateFigureCommand(
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

    /**
     * 생성자 - 컬렉션 타입의 방어적 복사 수행
     */
    public UpdateFigureCommand {
        // 컬렉션 타입 필드의 방어적 복사
        education = education != null ? List.copyOf(education) : List.of();
        careers = careers != null ? List.copyOf(careers) : List.of();
        sites = sites != null ? List.copyOf(sites) : List.of();
        activities = activities != null ? List.copyOf(activities) : List.of();
    }

    /**
     * 요청 객체로부터 명령 객체 생성
     * @param request 컨트롤러에서 받은 요청 객체
     * @return 서비스 계층에서 사용할 명령 객체
     */
    public static UpdateFigureCommand from(UpdateFigureRequest request) {
        return new UpdateFigureCommand(
                request.name(),
                request.englishName(),
                request.birth(),
                request.constituency(),
                request.profileUrl(),
                request.figureType(),
                request.figureParty(),
                request.education(),
                request.careers(),
                request.sites(),
                request.activities(),
                request.updateSource() != null ? request.updateSource() : "사용자 수정"
        );
    }

    /**
     * Figure 엔티티로부터 명령 객체 생성
     * 기존 정보를 기반으로 수정할 때 사용
     * @param figure 기존 국회의원 엔티티
     * @return 엔티티 정보가 담긴 명령 객체
     */
    public static UpdateFigureCommand fromEntity(Figure figure) {
        return new UpdateFigureCommand(
                figure.getName(),
                figure.getEnglishName(),
                figure.getBirth(),
                figure.getConstituency(),
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

    /**
     * FigureDTO로부터 명령 객체 생성
     * @param dto 국회의원 DTO
     * @return 명령 객체
     */
    public static UpdateFigureCommand fromDto(FigureDTO dto) {
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
            careerList = List.of();
        }

        return new UpdateFigureCommand(
                dto.getName(),
                dto.getEnglishName(),
                dto.getBirth(),
                dto.getConstituency(),
                dto.getProfileUrl(),
                dto.getFigureType(),
                dto.getFigureParty(),
                dto.getEducation(),
                careerList,
                dto.getSites(),
                dto.getActivities(),
                "DTO로부터 변환"
        );
    }

}

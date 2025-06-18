package com.example.GateStatus.domain.figure.service.request;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;

import java.util.List;
import java.util.Optional;

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
                Optional.ofNullable(request.updateSource()).orElse("사용자 수정")
        );
    }
}

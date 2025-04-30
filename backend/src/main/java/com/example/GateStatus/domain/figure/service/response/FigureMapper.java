package com.example.GateStatus.domain.figure.service.response;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureMapper {

    public void updateFigureFromDTO(Figure figure, FigureInfoDTO dto) {
        figure.update(
                dto.name(),
                dto.englishName(),
                dto.birth(),
                dto.constituency(),  // 지역구를 place로 사용
                dto.profileUrl(),
                FigureType.POLITICIAN,  // 국회의원은 기본적으로 POLITICIAN
                dto.partyName(),        // FigureParty 열거형
                convertEducation(dto),  // 학력 정보 변환
                convertCareers(dto),    // Career 객체로 변환
                convertSites(dto),      // 사이트 URL 변환
                convertActivities(dto), // 활동 내역 변환
                "국회 Open API"         // 업데이트 소스
        );
    }


    private List<String> convertEducation(FigureInfoDTO dto) {
        return dto.education() != null ? dto.education() : new ArrayList<>();
    }


    private List<Career> convertCareers(FigureInfoDTO dto) {
        List<Career> careers = new ArrayList<>();

        if (dto.electedCount() != null && !dto.electedCount().isEmpty()) {
            careers.add(Career.builder()
                    .title(dto.electedCount() + "대 국회의원")
                    .position("국회의원")
                    .organization("대한민국 국회")
                    .period(dto.electedDate() != null ? dto.electedDate() + " ~ 현재": "")
                    .build());
        }

        if (dto.committeeName() != null && !dto.committeeName().isEmpty()) {
            String position = dto.committeePosition() != null ? dto.committeePosition() : "위원";

            careers.add(Career.builder()
                    .title("국회 " + dto.committeeName())
                    .position(position)
                    .organization(dto.committeeName())
                    .period("현재")
                    .build());
        }

        // 기존 경력 정보가 이미 Career 리스트라면 그대로 추가
        if (dto.career() != null && !dto.career().isEmpty() && dto.career().get(0) instanceof Career) {
            careers.addAll((List<Career>) dto.career());
        }
        return careers;
    }

    private Career parseCareerText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return Career.builder()
                .title(text.trim())
                .position("")
                .organization("")
                .period("")
                .build();
    }


    private List<String> convertSites(FigureInfoDTO dto) {
        List<String> sites = new ArrayList<>();

        if (dto.homepage() != null && !dto.homepage().trim().isEmpty()) {
            sites.add(dto.homepage().trim());
        }

        if (dto.blog() != null && !dto.blog().trim().isEmpty()) {
            sites.add(dto.blog().trim());
        }

        if (dto.facebook() != null && !dto.facebook().trim().isEmpty()) {
            sites.add(dto.facebook().trim());
        }

        // 이메일 주소는 mailto: 프로토콜로 추가
        if (dto.email() != null && !dto.email().trim().isEmpty()) {
            sites.add("mailto:" + dto.email().trim());
        }
        return sites;
    }

    private List<String> convertActivities(FigureInfoDTO dto) {
        List<String> activities = new ArrayList<>();

        if (dto.electedCount() != null && !dto.electedCount().isEmpty()) {
            activities.add(dto.electedCount() + "대 국회의원");
        }

        if (dto.committeeName() != null && !dto.committeeName().isEmpty()) {
            String position = dto.committeePosition() != null ? dto.committeePosition() : "위원";
            activities.add(dto.committeeName() + " " + position);
        }

        return activities;
    }
}

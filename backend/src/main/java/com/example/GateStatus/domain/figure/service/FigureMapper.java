package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.career.CareerParser;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureMapper {
    private final CareerParser careerParser;

    public List<FigureInfoDTO> mapFromJsonNode(JsonNode dataArray) {
        if (dataArray == null || !dataArray.isArray()) {
            return Collections.emptyList();
        }

        List<FigureInfoDTO> result = new ArrayList<>();
        for (JsonNode row : dataArray) {
            String figureId = getTextValue(row, "MONA_CD");
            String name = getTextValue(row, "HG_NM");

            if (isEmpty(figureId)) {
                log.warn("유효하지 않은 figureId: {}", figureId);
                continue;
            }

            if (isEmpty(name)) {
                log.warn("유효하지 않은 name: {}", name);
                continue;
            }


            // 기존 로직 그대로 유지
            String englishName = getTextValue(row, "ENG_NM");
            String birth = getTextValue(row, "BTH_DATE");
            String partyNameStr = getTextValue(row, "POLY_NM");
            String constituency = getTextValue(row, "ORIG_NM");
            String committeeName = getTextValue(row, "CMIT_NM");
            String committeePosition = getTextValue(row, "JOB_RES_NM");
            String electedCount = getTextValue(row, "REELE_GBN_NM");
            String electedDate = getTextValue(row, "UNITS");
            String reelection = getTextValue(row, "REELE_GBN_NM");
            String email = getTextValue(row, "E_MAIL");
            String homepage = getTextValue(row, "HOMEPAGE");

            FigureParty partyName = convertToFigureParty(partyNameStr);

            List<String> education = parseEducation(row);
            List<Career> careers = parseCareers(row);

            FigureInfoDTO dto = new FigureInfoDTO(
                    figureId, name, englishName, birth, partyName, constituency,
                    committeeName, committeePosition, electedCount, electedDate,
                    reelection, null,
                    education,
                    careers,
                    email, homepage, null, null);

            result.add(dto);
        }
        return result;
    }


    public void updateFigureFromDTO(Figure figure, FigureInfoDTO dto) {
        // 중요: figureId가 없으면 업데이트하지 않도록
        if (figure.getFigureId() == null || figure.getFigureId().isEmpty()) {
            if (dto.figureId() != null && !dto.figureId().isEmpty()) {
                figure.setFigureId(dto.figureId());
            } else {
                // 둘 다 없으면 임시 ID 생성
                figure.setFigureId("TEMP_" + UUID.randomUUID().toString());
            }
        }

        figure.setName(dto.name());
        figure.setEnglishName(dto.englishName());
        figure.setBirth(dto.birth());
        figure.setConstituency(dto.constituency());
        figure.setFigureParty(dto.partyName());
        figure.setUpdateSource("국회 Open API");


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

    private List<Career> parseCareers(JsonNode row) {
        String careerText = getTextValue(row, "MEM_TITLE");

        if (isEmpty(careerText)) {
            return new ArrayList<>();
        }

        return careerParser.parseCareers(careerText);
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }


    private List<String> parseEducation(JsonNode row) {
        List<String> education = new ArrayList<>();

        addNonEmptyValue(education, getTextValue(row, "EDU1"));
        addNonEmptyValue(education, getTextValue(row, "EDU2"));
        addNonEmptyValue(education, getTextValue(row, "EDU3"));

        return education;
    }

    private void addNonEmptyValue(List<String> education, String edu3) {
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

    private FigureParty convertToFigureParty(String partyName) {
        if (partyName == null || partyName.isEmpty()) {
            return FigureParty.OTHER;
        }

        return switch (partyName.trim()) {
            case "더불어민주당" -> FigureParty.DEMOCRATIC;
            case "국민의힘" -> FigureParty.PEOPLE_POWER;
            case "조국혁신당" -> FigureParty.REBUILDING_KOR;
            case "정의당" -> FigureParty.JUSTICE;
            case "국민의당" -> FigureParty.PEOPLES;
            case "기본소득당" -> FigureParty.BASIC_INCOME;
            case "시대전환" -> FigureParty.TIME_TRANSITION;
            case "무소속" -> FigureParty.INDEPENDENT;
            default -> FigureParty.OTHER;
        };
    }


    /**
     * JsonNode에서 텍스트 값 추출
     */
    public String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }
}

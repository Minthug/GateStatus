package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.career.CareerDTO;
import com.example.GateStatus.domain.career.CareerParser;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.domain.figure.service.response.Figuredto;
import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FigureApiMapper implements ApiMapper<JsonNode, List<FigureInfoDTO>> {

    private final ObjectMapper objectMapper;
    private final CareerParser careerParser;

    @Override
    public List<FigureInfoDTO> map(AssemblyApiResponse<JsonNode> response) {
        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode dataArray = response.data();
            List<FigureInfoDTO> result = new ArrayList<>();

            if (dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    String partyNameStr = getTextValue(item, "POLY_MM");
                    String careersText = getTextValue(item, "MEM_TITLE");
                    FigureParty partyName = convertToFigureParty(partyNameStr);

                    FigureInfoDTO dto = new FigureInfoDTO(
                            getTextValue(item, "MONA_CD"),
                            getTextValue(item, "HG_NM"),
                            getTextValue(item, "ENG_NM"),
                            getTextValue(item, "BTH_DATE"),
                            partyName,
                            getTextValue(item, "ORIG_NM"),
                            getTextValue(item, "CMIT_NM"),
                            getTextValue(item, "CMITS_MEMB_JOB"),
                            getTextValue(item, "REELE_GBN_NM"),
                            getTextValue(item, "UNITS"),
                            getTextValue(item, "REELE_NM"),
                            getTextValue(item, "IMAGE_URL"),
                            parseEducation(item),
                            careerParser.parseCareers(careersText),
                            getTextValue(item, "EMAIL"),
                            getTextValue(item, "HOMEPAGE"),
                            getTextValue(item, "BLOG_URL"),
                            getTextValue(item, "FACEBOOK"));

                    result.add(dto);
                }
            }
            return result;
        } catch (Exception e) {
            throw new ApiMappingException("국회의원 정보 매핑 중 오류 발생");
        }
    }

    private FigureParty convertToFigureParty(String partyName) {
        if (partyName == null || partyName.isEmpty()) {
            return FigureParty.OTHER;
        }

        return switch (partyName.trim()) {
            case "더불어민주당" -> FigureParty.DEMOCRATIC;
            case "국민의힘" -> FigureParty.PEOPLE_POWER;
            case "정의당" -> FigureParty.JUSTICE;
            case "국민의당" -> FigureParty.PEOPLES;
            case "기본소득당" -> FigureParty.BASIC_INCOME;
            case "시대전환" -> FigureParty.TIME_TRANSITION;
            case "무소속" -> FigureParty.INDEPENDENT;
            default -> FigureParty.OTHER;
        };
    }

    private List<String> parseEducation(JsonNode item) {
        String edu = getTextValue(item, "EDU");
        if (edu == null || edu.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(edu.split("\\n|\\r\\n|,|;"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public FigureDTO convertToFigureDTO(FigureInfoDTO dto) {
        if (dto == null) return null;

        return FigureDTO.builder()
                .figureId(dto.figureId())
                .name(dto.name())
                .englishName(dto.englishName())
                .birth(dto.birth())
                .constituency(dto.constituency())
                .profileUrl(dto.profileUrl())
                .figureType(FigureType.POLITICIAN)
                .figureParty(dto.partyName())
                .education(dto.education())
                .careers(convertCareersToDTO(dto.career()))
                .sites(dto.getLinkUrl())
                .activities(dto.getActivities())
                .viewCount(0L)
                .build();
    }

    private List<CareerDTO> convertCareersToDTO(List<Career> careers) {
        if (careers == null) return new ArrayList<>();

        return careers.stream()
                .map(career -> CareerDTO.builder()
                        .period(career.getPeriod())
                        .position(career.getPosition())
                        .organization(career.getOrganization())
                        .title(career.getTitle())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * JsonNode에서 텍스트 값 추출
     */
    public String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }
}

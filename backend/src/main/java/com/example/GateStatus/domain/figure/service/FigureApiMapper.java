package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
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
                            parseCareer(careersText),
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

    private List<Career> parseCareer(String careersText) {
        List<Career> careers = new ArrayList<>();
        if (careersText == null || careersText.isEmpty()) {
            return careers;
        }

        String[] lines = careersText.split("\r\n");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            try {
                // 경력 정보 파싱 로직
                String[] parts = line.trim().split(" / ");

                if (parts.length < 2) {
                    log.warn("경력 정보 파싱 실패: {}", line);
                    continue;
                }

                // 기간 파싱
                String[] periodParts = parts[0].split(" ~ ");
                LocalDate startDate = parseDateOrNull(periodParts[0]);
                LocalDate endDate = periodParts.length > 1 && !"현재".equals(periodParts[1])
                        ? parseDateOrNull(periodParts[1])
                        : null;

                // 직위와 소속 결정
                String position = parts.length > 1 ? parts[1] : "";
                String organization = parts.length > 2 ? parts[2] : "";

                Career career = Career.of(
                        line,           // title
                        position,       // position
                        organization,   // organization
                        startDate,      // startDate
                        endDate         // endDate
                );

                careers.add(career);
            } catch (Exception e) {
                log.warn("경력 정보 파싱 중 오류: {}, 오류: {}", line, e.getMessage());
            }
        }

        return careers;
    }
    private LocalDate parseDateOrNull(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || "현재".equals(dateStr.trim())) {
            return null;
        }

        try {
            // 다양한 날짜 형식 지원
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy.MM"),
                    DateTimeFormatter.ofPattern("yyyy-MM"),
                    DateTimeFormatter.ofPattern("yyyy/MM")
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr.trim() + "-01",
                            formatter.withResolverStyle(ResolverStyle.STRICT));
                } catch (DateTimeParseException e) {
                    // 현재 포맷터로 파싱 실패하면 다음 포맷터 시도
                }
            }

            log.warn("날짜 파싱 실패: {}", dateStr);
            return null;
        } catch (Exception e) {
            log.warn("날짜 파싱 중 오류: {}, 오류: {}", dateStr, e.getMessage());
            return null;
        }
    }

    /**
     * JsonNode에서 텍스트 값 추출
     */
    public String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }
}

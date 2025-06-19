package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.career.CareerParser;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.GateStatus.domain.common.JsonUtils.getTextValue;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureMapper implements ApiMapper<JsonNode, List<FigureInfoDTO>> {
    private final CareerParser careerParser;
    private final ObjectMapper objectMapper;

    @Override
    public List<FigureInfoDTO> map(AssemblyApiResponse<JsonNode> response) {
        if (response == null || response.data() == null) {
            log.warn("API 응답이 null이거나 데이터가 없습니다");
            return Collections.emptyList();
        }

        try {
            JsonNode dataArray = response.data();
            return mapFromJsonNode(dataArray);
        } catch (Exception e) {
            log.error("API 응답 매핑 중 오류 발생: {}", e.getMessage(), e);
            throw new ApiMappingException("국회의원 정보 매핑 중 오류 발생: " + e.getMessage());
        }
    }

    public List<FigureInfoDTO> mapFromJsonNode(JsonNode dataArray) {
        if (dataArray == null || !dataArray.isArray()) {
            log.warn("데이터 배열이 null이거나 배열 타입이 아닙니다");
            return Collections.emptyList();
        }

        List<FigureInfoDTO> result = new ArrayList<>();

        for (JsonNode row : dataArray) {
            try {
                FigureInfoDTO dto = mapSingleNode(row);
                if (dto != null) {
                    result.add(dto);
                }
            } catch (Exception e) {
                log.warn("개별 노드 매핑 실패, 건너뜀: {}", e.getMessage());
            }
        }
        log.info("총 {}개 노드 중 {}개 성공적으로 매핑", dataArray.size(), result.size());
        return result;
    }

    private FigureInfoDTO mapSingleNode(JsonNode row) {
        String figureId = getTextValue(row, "MONA_CD");
        String name = getTextValue(row, "HG_NM");

        if (isEmpty(figureId)) {
            log.warn("유효하지 않은 figureId: {}", figureId);
            return null;
        }

        if (isEmpty(name)) {
            log.warn("유효하지 않은 name: {}", name);
            return null;
        }

        // 기본 정보 추출
        String englishName = getTextValue(row, "ENG_NM");
        String birth = getTextValue(row, "BTH_DATE");
        String partyNameStr = getTextValue(row, "POLY_NM");
        String constituency = getTextValue(row, "ORIG_NM");
        String committeeName = getTextValue(row, "CMIT_NM");
        String committeePosition = getTextValue(row, "JOB_RES_NM");
        String electedCount = getTextValue(row, "REELE_GBN_NM");
        String electedDate = getTextValue(row, "UNITS");
        String reelection = getTextValue(row, "REELE_GBN_NM");
        String profileUrl = getTextValue(row, "IMAGE_URL");
        String email = getTextValue(row, "E_MAIL");
        String homepage = getTextValue(row, "HOMEPAGE");
        String blog = getTextValue(row, "BLOG_URL");
        String facebook = getTextValue(row, "FACEBOOK");

        // 변환된 데이터
        FigureParty partyName = convertToFigureParty(partyNameStr);
        List<String> education = parseEducation(row);
        List<Career> careers = parseCareers(row);


        return new FigureInfoDTO(
                figureId, name, englishName, birth, partyName, constituency,
                committeeName, committeePosition, electedCount, electedDate,
                reelection, profileUrl,
                education, careers,
                email, homepage, blog, facebook
        );
    }



    public void updateFigureFromDTO(Figure figure, FigureInfoDTO dto) {
        validateAndSetFigureId(figure, dto);
        updateBasicFields(figure, dto);
        updateComplexFields(figure, dto);
    }

    private void validateAndSetFigureId(Figure figure, FigureInfoDTO dto) {
        if (figure.getFigureId() == null || figure.getFigureId().isEmpty()) {
            if (dto.figureId() != null && !dto.figureId().isEmpty()) {
                figure.setFigureId(dto.figureId());
            } else {
                String tempId = "TEMP_" + UUID.randomUUID().toString();
                figure.setFigureId(tempId);
                log.warn("FigureId가 없어 임시 ID 생성: {}", tempId);
            }
        }
    }


    private void updateBasicFields(Figure figure, FigureInfoDTO dto) {
        figure.setName(dto.name());
        figure.setEnglishName(dto.englishName());
        figure.setBirth(dto.birth());
        figure.setConstituency(dto.constituency());
        figure.setFigureParty(dto.partyName());
        figure.setProfileUrl(dto.profileUrl());
        figure.setUpdateSource("국회 Open API");
    }

    private void updateComplexFields(Figure figure, FigureInfoDTO dto) {
        figure.update(
                dto.name(),
                dto.englishName(),
                dto.birth(),
                dto.constituency(),
                dto.profileUrl(),
                FigureType.POLITICIAN,
                dto.partyName(),
                convertEducation(dto),
                convertCareers(dto),
                convertSites(dto),
                convertActivities(dto),
                "국회 Open API"
        );
    }

    // ========== DTO 변환 관련 메서드 ==========
    public FigureDTO convertToFigureDTO(FigureInfoDTO dto) {
        if (dto == null) {
            log.warn("변환할 DTO가 null입니다");
            return null;
        }
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

    // ========== 데이터 변환 유틸리티 메서드 ==========

    private List<String> convertEducation(FigureInfoDTO dto) {
        return Optional.ofNullable(dto.education()).orElse(new ArrayList<>());
    }

    private List<Career> convertCareers(FigureInfoDTO dto) {
        List<Career> careers = new ArrayList<>();

        addAssemblyCareer(careers, dto);

        addCommitteeCareer(careers, dto);

        addExistingCareers(careers, dto);

        return careers;
    }

    /**
     * 국회의원 기본 경력 추가
     * @param careers 경력 리스트
     * @param dto FigureInfoDTO
     */
    private void addAssemblyCareer(List<Career> careers, FigureInfoDTO dto) {
        if (isNotEmpty(dto.electedCount())) {
            careers.add(Career.builder()
                    .title(dto.electedCount() + "대 국회의원")
                    .position("국회의원")
                    .organization("대한민국 국회")
                    .period(isNotEmpty(dto.electedDate()) ? dto.electedDate() + " ~ 현재" : "현재")
                    .build());
        }
    }

    /**
     * 위원회 경력 추가
     * @param careers 경력 리스트
     * @param dto FigureInfoDTO
     */
    private void addCommitteeCareer(List<Career> careers, FigureInfoDTO dto) {
        if (isNotEmpty(dto.committeeName())) {
            String position = isNotEmpty(dto.committeePosition()) ? dto.committeePosition() : "위원";
            careers.add(Career.builder()
                    .title("국회 " + dto.committeeName())
                    .position(position)
                    .organization(dto.committeeName())
                    .period("현재")
                    .build());
        }
    }

    /**
     * 기존 경력 정보 추가
     * @param careers 경력 리스트
     * @param dto FigureInfoDTO
     */
    @SuppressWarnings("unchecked")
    private void addExistingCareers(List<Career> careers, FigureInfoDTO dto) {
        if (dto.career() != null && !dto.career().isEmpty()) {
            try {
                // 타입 안전성 검사
                if (dto.career().get(0) instanceof Career) {
                    careers.addAll((List<Career>) dto.career());
                }
            } catch (Exception e) {
                log.warn("기존 경력 정보 추가 중 오류: {}", e.getMessage());
            }
        }
    }

    private List<String> convertSites(FigureInfoDTO dto) {
        List<String> sites = new ArrayList<>();

        addSiteIfNotEmpty(sites, dto.homepage());
        addSiteIfNotEmpty(sites, dto.blog());
        addSiteIfNotEmpty(sites, dto.facebook());

        // 이메일 주소는 mailto: 프로토콜로 추가
        if (isNotEmpty(dto.email())) {
            sites.add("mailto:" + dto.email().trim());
        }

        return sites;
    }

    private void addSiteIfNotEmpty(List<String> sites, String url) {
        if (isNotEmpty(url)) {
            sites.add(url.trim());
        }
    }


    private List<String> convertActivities(FigureInfoDTO dto) {
        List<String> activities = new ArrayList<>();

        if (isNotEmpty(dto.electedCount())) {
            activities.add(dto.electedCount() + "대 국회의원");
        }

        if (isNotEmpty(dto.committeeName())) {
            String position = isNotEmpty(dto.committeePosition()) ? dto.committeePosition() : "위원";
            activities.add(dto.committeeName() + " " + position);
        }

        return activities;
    }

    // ========== JSON 파싱 관련 메서드 ==========

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

    private boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    private List<String> parseEducation(JsonNode row) {
        String singleEdu = getTextValue(row, "EDU");
        if (isNotEmpty(singleEdu)) {
            return Arrays.stream(singleEdu.split("\\n|\\r\\n|,|;"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<String> education = new ArrayList<>();
        addNonEmptyEducation(education, getTextValue(row, "EDU1"));
        addNonEmptyEducation(education, getTextValue(row, "EDU2"));
        addNonEmptyEducation(education, getTextValue(row, "EDU3"));

        return education;
    }

    private void addNonEmptyEducation(List<String> education, String eduValue) {
        if (isNotEmpty(eduValue)) {
            education.add(eduValue.trim());
        }
    }

    // ========== 정당 변환 메서드 ==========

    private FigureParty convertToFigureParty(String partyName) {
        if (isEmpty(partyName)) {
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
            default -> {
                log.debug("알 수 없는 정당명: {}", partyName);
                yield FigureParty.OTHER;
            }
        };
    }
}

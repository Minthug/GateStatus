package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.career.CareerDTO;
import com.example.GateStatus.domain.career.CareerParser;
import com.example.GateStatus.domain.common.JsonUtils;
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
import java.util.stream.Stream;

import static com.example.GateStatus.domain.common.JsonUtils.*;

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

    // ========== Entity 업데이트 관련 메서드 ==========
    public void updateFigureFromDTO(Figure figure, FigureInfoDTO dto) {
        validateAndSetFigureId(figure, dto);

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

    // ========== DTO ↔ Entity 변환 관련 메서드 ==========
    public Figure convertDtoToResponseEntity(FigureDTO dto) {
        if (dto == null) {
            log.warn("변환할 FigureDTO가 null입니다");
            return null;
        }

        return Figure.builder()
                .id(null)
                .figureId(dto.getFigureId())
                .name(dto.getName())
                .englishName(dto.getEnglishName())
                .birth(dto.getBirth())
                .constituency(dto.getConstituency())
                .profileUrl(dto.getProfileUrl())
                .figureType(dto.getFigureType())
                .figureParty(dto.getFigureParty())
                .education(safeListCopy(dto.getEducation()))
                .careers(convertCareersDtosToEntities(dto.getCareers()))
                .sites(safeListCopy(dto.getSites()))
                .activities(safeListCopy(dto.getActivities()))
                .viewCount(dto.getViewCount())
                .build();

    }

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

    private List<Career> convertCareersDtosToEntities(List<CareerDTO> careers) {
        if (careers == null) {
            return new ArrayList<>();
        }

        return careers.stream()
                .map(this::convertCareersDtoToEntity)
                .collect(Collectors.toList());
    }

    private Career convertCareersDtoToEntity(CareerDTO careerDTO) {
        return Career.builder()
                .title(careerDTO.getTitle())
                .position(careerDTO.getPosition())
                .organization(careerDTO.getOrganization())
                .period(careerDTO.getPeriod())
                .build();
    }

    // ========== 데이터 변환 유틸리티 메서드 ==========
    private List<CareerDTO> convertCareersToDTO(List<Career> careers) {
        return Optional.ofNullable(careers)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::convertSingleCareerToDTO)
                .collect(Collectors.toList());
    }

    private CareerDTO convertSingleCareerToDTO(Career career) {
        return CareerDTO.builder()
                .title(career.getTitle())
                .position(career.getPosition())
                .organization(career.getOrganization())
                .period(career.getPeriod())
                .build();
    }

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

        // URL들을 정규화해서 추가
        Stream.of(dto.homepage(), dto.blog(), dto.facebook())
                .filter(JsonUtils::isNotEmpty)
                .map(this::normalizeUrl)
                .forEach(sites::add);

        // 이메일은 정규화해서 mailto: 형식으로 추가
        if (isNotEmpty(dto.email())) {
            String normalizedEmail = normalizeEmail(dto.email());
            if (isNotEmpty(normalizedEmail)) {
                sites.add("mailto:" + normalizedEmail);
            }
        }

        return sites;
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

    private List<String> parseEducation(JsonNode row) {
        Set<String> educationSet = new LinkedHashSet<>();

        String singleEdu = getTextValue(row, "EDU");
        if (isNotEmpty(singleEdu)) {
            safeSplit(singleEdu, "\\n|\\r\\n|,|;").forEach(educationSet::add);
        }

        // 개별 교육 필드들에서 추출
        Stream.of("EDU1", "EDU2", "EDU3")
                .map(field -> getTextValue(row, field))
                .filter(JsonUtils::isNotEmpty)
                .forEach(educationSet::add);

        return new ArrayList<>(educationSet);
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

    /**
     * 안전한 문자열 분할
     * @param text 분할할 텍스트
     * @param delimiter 구분자
     * @return 분할된 문자열 리스트
     */
    private List<String> safeSplit(String text, String delimiter) {
        if (JsonUtils.isEmpty(text)) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.split(delimiter))
                .map(String::trim)
                .filter(JsonUtils::isNotEmpty)
                .collect(Collectors.toList());
    }


    /**
     * 전화번호 형식 정규화
     * @param phoneNumber 원본 전화번호
     * @return 정규화된 전화번호
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (isEmpty(phoneNumber)) {
            return "";
        }

        // 숫자만 추출
        String numbersOnly = phoneNumber.replaceAll("[^0-9]", "");

        // 형식에 맞춰 변환 (예: 02-1234-5678)
        if (numbersOnly.length() >= 10) {
            if (numbersOnly.startsWith("02")) {
                return numbersOnly.replaceFirst("(\\d{2})(\\d{3,4})(\\d{4})", "$1-$2-$3");
            } else if (numbersOnly.length() == 11) {
                return numbersOnly.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
            }
        }

        return phoneNumber;
    }

    /**
     * URL 유효성 검사 및 정규화
     * @param url 검사할 URL
     * @return 유효한 URL 또는 빈 문자열
     */
    private String normalizeUrl(String url) {
        if (isEmpty(url)) {
            return "";
        }

        String trimmedUrl = url.trim();

        // http:// 또는 https://가 없으면 추가
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            trimmedUrl = "https://" + trimmedUrl;
        }

        try {
            // URL 유효성 검사
            new java.net.URL(trimmedUrl);
            return trimmedUrl;
        } catch (Exception e) {
            log.warn("URL 형식이 올바르지 않음: {}", url);
            return url; // 원본 반환
        }
    }

    /**
     * 이메일 유효성 검사
     * @param email 검사할 이메일
     * @return 유효한 이메일 또는 빈 문자열
     */
    private String normalizeEmail(String email) {
        if (isEmpty(email)) {
            return "";
        }

        String trimmedEmail = email.trim().toLowerCase();

        // 간단한 이메일 형식 검사
        if (trimmedEmail.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            return trimmedEmail;
        }

        log.warn("이메일 형식이 올바르지 않음: {}", email);
        return email; // 원본 반환
    }

    /**
     * 리스트의 방어적 복사 수행
     * @param source 원본 리스트
     * @return 복사된 리스트 (null인 경우 빈 리스트)
     */
    private <T> List<T> safeListCopy(List<T> source) {
        return source != null ? new ArrayList<>(source) : new ArrayList<>();
    }
}

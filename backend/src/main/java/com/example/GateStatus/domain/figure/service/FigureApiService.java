package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.career.CareerParser;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureApiService {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final FigureRepository figureRepository;
    private final FigureMapper figureMapper;
    private final CareerParser careerParser;
    private final FigureCacheService figureCacheService;

    @PersistenceContext
    private final EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apiKey;

    @Value("${spring.openapi.assembly.figure-api-path}")
    private String figureApiPath;


    @Transactional
    public Figure syncFigureInfoByName(String figureName) {
        FigureInfoDTO info = fetchFigureInfoFromApi(figureName);

        if (info == null) {
            throw new EntityNotFoundException("해당 이름의 정치인을 찾을 수 없습니다");
        }

        Figure figure = figureRepository.findByName(figureName)
                .orElseGet(() -> Figure.builder()
                        .name(figureName)
                        .figureType(FigureType.POLITICIAN)
                        .viewCount(0L)
                        .build());

        figureMapper.updateFigureFromDTO(figure, info);

        return figureRepository.save(figure);
    }

    private FigureInfoDTO fetchFigureInfoFromApi(String figureName) {
        log.info("국회의원 정보 API 호출 시작: {}", figureName);

        try {
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("HG_NM", figureName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (isEmpty(jsonResponse)) {
                log.warn("API에서 빈 응답 또는 null 반환 (이름: {}) ", figureName);
                return null;
            }

            log.debug("API 응답 수신 일부: {}", jsonResponse.substring(0, Math.min(100, jsonResponse.length())));

            List<FigureInfoDTO> figures = parseJsonResponse(jsonResponse);
            log.info("국회의원 정보 조회 결과: {} 명 ", figures.size());

            return figures.isEmpty() ? null : figures.get(0);
        } catch (Exception e) {
            log.error("국회의원 정보 조회 중 오류: {} - {} ", figureName, e.getMessage(), e);
            throw new ApiDataRetrievalException("국회의원 정보를 가져오는 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 모든 국회의원 정보를 동기화 합니다
     *
     * @return
     */
    public int syncAllFiguresV3() {
        log.info("모든 국회의원 정보 동기화 시작");
        List<FigureInfoDTO> allFigures = fetchAllFiguresFromAPiV2();

        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return 0;
        }

        log.info("동기화 대상 국회의원: {}명", allFigures.size());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        int successCount = 0;
        int failCount = 0;

        for (FigureInfoDTO figure : allFigures) {
            try {
                // 기본 정보만 저장 (트랜잭션 내에서)
                boolean basicSaved = saveBasicFigureInfo(figure);

                if (basicSaved) {
                    // 트랜잭션 내에서 기본 정보가 저장된 후에 컬렉션 업데이트 수행
                    try {
                        updateEducation(figure);
                        updateCareers(figure);
                        updateSites(figure);
                        updateActivities(figure);

                        successCount++;
                        log.info("국회의원 정보 동기화 성공: {}", figure.name());
                    } catch (Exception e) {
                        log.error("컬렉션 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage(), e);
                        failCount++;
                    }
                } else {
                    failCount++;
                    log.warn("국회의원 기본 정보 저장 실패: {}", figure.name());
                }
            } catch (Exception e) {
                log.error("국회의원 처리 중 예외 발생: {} - {}", figure.name(), e.getMessage(), e);
                failCount++;
            }
            }

        log.info("국회의원 정보 동기화 완료: 총 {}명 중 {}명 성공, {}명 실패", allFigures.size(), successCount, failCount);
        return successCount;
    }

    /**
     * 단일 국회의원 저장 메서드 (별도 트랜잭션으로 분리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveBasicFigureInfo(FigureInfoDTO figureDTO) {
        try {
            log.info("국회의원 저장 시작: {}", figureDTO.name());

            // 1. 기존 엔티티 조회 또는 새로 생성
            Figure figure = figureRepository.findByFigureIdWithoutCollections(figureDTO.figureId())
                    .orElseGet(() ->
                            Figure.builder()
                            .figureId(figureDTO.figureId())
                            .name(figureDTO.name())
                            .figureType(FigureType.POLITICIAN)
                            .viewCount(0L)
                            .build());

            // 기본 정보만 업데이트
            figure.setName(figureDTO.name());
            figure.setEnglishName(figureDTO.englishName());
            figure.setBirth(figureDTO.birth());
            figure.setConstituency(figureDTO.constituency());
            figure.setFigureParty(figureDTO.partyName());
            figure.setUpdateSource("국회 Open API");

            // 저장
            Figure savedFigure = figureRepository.save(figure);

        // EntityManager로 직접 조회
            boolean exists = false;
            try {
                Figure found = entityManager.createQuery(
                                "SELECT f FROM Figure f WHERE f.figureId = :id", Figure.class)
                        .setParameter("id", figureDTO.figureId())
                        .getSingleResult();
                exists = (found != null);
            } catch (Exception e) {
                log.warn("엔티티 조회 실패: {}", e.getMessage());
            }

            if (exists) {
                log.info("국회의원 저장 성공: {}, ID={}", figureDTO.name(), figureDTO.figureId());
                return true;
            } else {
                log.warn("국회의원 저장 실패 (직접 조회 불가): {}", figureDTO.name());
                return false;
            }
        } catch (Exception e) {
            log.error("국회의원 저장 실패: {} - {}", figureDTO.name(), e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외 다시 던지기
        }
    }

    public int syncAllFiguresV4() {
        log.info("모든 국회의원 정보 동기화 시작 V4");
        List<FigureInfoDTO> allFigures = fetchAllFiguresFromAPiV2();

        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return 0;
        }

        log.info("동기화 대상 국회의원: {}명", allFigures.size());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        int successCount = 0;
        int failCount = 0;

        for (FigureInfoDTO figure : allFigures) {
            try {
                Boolean result = transactionTemplate.execute(status -> {
                    try {
                        boolean exists = figureRepository.existsByFigureId(figure.figureId());

                        if (exists) {
                            updateFigureBasicInfo(figure);
                        } else {
                            insertFigureBasicInfo(figure);
                        }

                        updateCollectionsWithNativeSql(figure);

                        return true;
                    } catch (Exception e) {
                        log.error("국회의원 저장 중 오류: {} - {}", figure.name(), e.getMessage(), e);
                        status.setRollbackOnly();
                        return false;
                    }
                });

                if (Boolean.TRUE.equals(result)) {
                    successCount++;
                    log.info("국회의원 정보 동기화 성공: {}", figure.name());
                } else {
                    failCount++;
                    log.warn("국회의원 정보 동기화 실패: {}", figure.name());
                }
            } catch (Exception e) {
                log.error("국회의원 처리 중 예외 발생: {} - {}", figure.name(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("국회의원 정보 동기화 완료: 총 {}명 중 {}명 성공, {}명 실패", allFigures.size(), successCount, failCount);
        return successCount;
    }

    private void updateFigureBasicInfo(FigureInfoDTO figure) {
        entityManager.createNativeQuery(
                "UPDATE figure SET " +
                        "name = ?, " +
                        "english_name = ?, " +
                        "birth = ?, " +
                        "constituency = ?, " +
                        "figure_party = ?, " +
                        "update_source = ?, " +
                        "WHERE figure_id = ?")
                .setParameter(1, figure.name())
                .setParameter(2, figure.englishName())
                .setParameter(3, figure.birth())
                .setParameter(4, figure.constituency())
                .setParameter(5, figure.partyName() != null ? figure.partyName().toString() : null)
                .setParameter(6, "국회 Open API")
                .setParameter(7, figure.figureId())
                .executeUpdate();

        log.debug("국회의원 기본 정보 업데이트 완료: {}", figure.name());
    }



    private void insertFigureBasicInfo(FigureInfoDTO figure) {
        entityManager.createNativeQuery(
                "INSERT INTO figure (figure_id, name, english_name, birth, constituency, figure_type, figure_party, view_count, update_source) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, figure.figureId())
                .setParameter(2, figure.name())
                .setParameter(3, figure.englishName())
                .setParameter(4, figure.birth())
                .setParameter(5, figure.constituency())
                .setParameter(6, "POLITICIAN")
                .setParameter(7, figure.partyName() != null ? figure.partyName().toString() : null)
                .setParameter(8, 0L)
                .setParameter(9, "국회 Open API")
                .executeUpdate();

        log.debug("새 국회의원 정보 삽입 완료: {}", figure.name());
    }

    private void updateCollectionsWithNativeSql(FigureInfoDTO figure) {
        String figureId = figure.figureId();

        try {
            entityManager.createNativeQuery("DELETE FROM figure_education WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM figure_career WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM figure_site WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM figure_activity WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            if (figure.education() != null) {
                for (String education : figure.education()) {
                    if (education != null && !education.trim().isEmpty()) {
                        entityManager.createNativeQuery(
                                        "INSERT INTO figure_education (figure_id, education) VALUES (?, ?)")
                                .setParameter(1, figureId)
                                .setParameter(2, education.trim())
                                .executeUpdate();
                    }
                }
            }

            // 경력 정보 삽입
            // 국회의원 기본 경력
            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_career (figure_id, title, position, organization, period) VALUES (?, ?, ?, ?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.electedCount() + "대 국회의원")
                        .setParameter(3, "국회의원")
                        .setParameter(4, "대한민국 국회")
                        .setParameter(5, figure.electedDate() != null ? figure.electedDate() + " ~ 현재" : "현재")
                        .executeUpdate();
            }

            // 위원회 경력
            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                entityManager.createNativeQuery(
                                "INSERT INTO figure_career (figure_id, title, position, organization, period) VALUES (?, ?, ?, ?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, "국회 " + figure.committeeName())
                        .setParameter(3, position)
                        .setParameter(4, figure.committeeName())
                        .setParameter(5, "현재")
                        .executeUpdate();
            }

            // 사이트 정보 삽입
            if (figure.homepage() != null && !figure.homepage().trim().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_site (figure_id, site) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.homepage().trim())
                        .executeUpdate();
            }

            if (figure.email() != null && !figure.email().trim().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_site (figure_id, site) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, "mailto:" + figure.email().trim())
                        .executeUpdate();
            }

            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_activity (figure_id, activity) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.electedCount() + "대 국회의원")
                        .executeUpdate();
            }

            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                entityManager.createNativeQuery(
                                "INSERT INTO figure_activity (figure_id, activity) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.committeeName() + " " + position)
                        .executeUpdate();
            }

            log.debug("컬렉션 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.error("컬렉션 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 모든 국회의원 정보를 API에서 가져옵니다
     *
     * @return
     */
    public List<FigureInfoDTO> fetchAllFiguresFromAPiV2() {
        log.info("전체 국회의원 정보 API 호출 시작");
        List<FigureInfoDTO> allFigures = new ArrayList<>();

        int maxPages = 4;

        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            try {
                log.info("국회의원 정보 API 호출: 페이지 {}", pageNo);
                int finalPageNo = pageNo;
                String jsonResponse = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(figureApiPath)
                                .queryParam("KEY", apiKey)
                                .queryParam("Type", "json")
                                .queryParam("pIndex", finalPageNo)
                                .queryParam("pSize", 100)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (isEmpty(jsonResponse)) {
                    log.error("API에서 빈 응답을 반환");
                    return Collections.emptyList();
                }

                log.info("API 응답 수신 (일부): {}", jsonResponse.substring(0, Math.min(100, jsonResponse.length())));

                JsonNode rootNode = mapper.readTree(jsonResponse);
                JsonNode rowsNode = rootNode.path("nwvrqwxyaytdsfvhu")
                        .path(1)
                        .path("row");

                if (!rowsNode.isArray() || rowsNode.size() == 0) {
                    log.info("페이지 {}에서 더 이상 데이터가 없습니다.", pageNo);
                    break;
                }

                // 매퍼를 사용하여 DTO로 변환
                List<FigureInfoDTO> pageFigures = figureMapper.mapFromJsonNode(rowsNode);
                log.info("페이지 {} 국회의원 정보 파싱 완료: {}명", pageNo, pageFigures.size());

                allFigures.addAll(pageFigures);

                if (pageFigures.size() < 100) {
                    log.info("마지막 페이지 도달 (페이지 {})", pageNo);
                    break;
                }
            } catch (Exception e) {
                log.error("전체 국회의원 정보 조회 중 오류: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }

        log.info("전체 국회의원 정보 API 호출 완료: 총 {}명", allFigures.size());
        return allFigures;
    }

    private List<FigureInfoDTO> parseJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode rowsNode = rootNode.path("nwvrqwxyaytdsfvhu")
                    .path(1)
                    .path("row");

            if (!rowsNode.isArray()) {
                log.warn("JSON 응답에서 row 배열을 찾을 수 없습니다");
                return Collections.emptyList();
            }

            List<FigureInfoDTO> figures = new ArrayList<>();
            int parsedCount = 0;
            int skipCount = 0;

            for (JsonNode row : rowsNode) {
                try {
                    FigureInfoDTO dto = parseFigureFromJsonNode(row);
                    if (dto != null) {
                        figures.add(dto);
                        parsedCount++;
                    } else {
                        skipCount++;
                    }
                } catch (Exception e) {
                    log.warn("국회의원 파싱 중 오류 발생: {}", e.getMessage());
                    skipCount++;
                }
            }

            log.info("국회의원 정보 파싱 완료: 성공 {}, 실패 {}", parsedCount, skipCount);
            return figures;
        } catch (Exception e) {
            log.error("JSON 파싱 중 오류 발생: {}", e.getMessage());
            throw new ApiDataRetrievalException("JSON 파싱 실패 " + e.getMessage());
        }
    }

    private FigureInfoDTO parseFigureFromJsonNode(JsonNode row) {
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
        String email = getTextValue(row, "E_MAIL");
        String homepage = getTextValue(row, "HOMEPAGE");

        FigureParty partyName = convertToFigureParty(partyNameStr);

        List<String> education = parseEducation(row);
        List<Career> careers = parseCareers(row);

        return new FigureInfoDTO(
                figureId, name, englishName, birth, partyName, constituency,
                committeeName, committeePosition, electedCount, electedDate,
                reelection, null,
                education,
                careers,
                email, homepage, null, null);
    }

    /**
     * 특정 정당 소속 국회의원 정보를 동기화합니다
     *
     * @param partyName
     * @return
     */
    @Transactional
    public int syncFigureByParty(String partyName) {
        try {
            log.info("{}당 소속 국회의원 정보 동기화 시작", partyName);
            List<FigureInfoDTO> partyFigures = fetchAllFiguresByPartyFromApi(partyName);
            int count = 0;

            for (FigureInfoDTO figureInfoDTO : partyFigures) {
                try {
                    Figure figure = figureRepository.findByName(figureInfoDTO.name())
                            .orElseGet(() -> Figure.builder()
                                    .name(figureInfoDTO.name())
                                    .figureType(FigureType.POLITICIAN)
                                    .viewCount(0L)
                                    .build());

                    figureMapper.updateFigureFromDTO(figure, figureInfoDTO);

                    figureRepository.save(figure);
                    count++;
                    log.debug("국회의원 정보 동기화 완료: {}", figureInfoDTO.name());

                } catch (Exception e) {
                    log.error("국회의원 동기화 중 오류 발생: {}", figureInfoDTO.name(), e);
                    // 개별 국회의원 동기화 오류는 무시하고 계속 진행
                }
            }

            log.info("{}당 소속 국회의원 정보 동기화 완료: {}명", partyName, count);
            return count;
        } catch (Exception e) {
            log.error("정당별 국회의원 동기화 중 오류 발생: {}", partyName, e);
            throw new ApiDataRetrievalException("정당별 국회의원 정보를 동기화하는 중 오류 발생");
        }
    }

    /**
     * 특정 정당 소속 국회의원 정보를 API에서 가져옵니다.
     *
     * @param partyName 정당 이름
     * @return 국회의원 정보 DTO 목록
     */
    private List<FigureInfoDTO> fetchAllFiguresByPartyFromApi(String partyName) {
        try {
            log.info("{}당 소속 국회의원 정보 API 호출 시작", partyName);

            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("POLY_NM", partyName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // JsonNode로 변환
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode dataArray = rootNode.path("nwvrqwxyaytdsfvhu")
                    .path(1)
                    .path("row");

            List<FigureInfoDTO> figures = figureMapper.mapFromJsonNode(dataArray);
            log.info("{}당 소속 국회의원 정보 API 호출 완료: {}명", partyName, figures.size());

            return figures;
        } catch (Exception e) {

            log.error("정당별 국회의원 정보 조회 중 오류 발생: {}", partyName, e);
            throw new ApiDataRetrievalException("정당별 국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    // 문자열을 FigureParty 열거형으로 변환
    private FigureParty convertToFigureParty(String partyName) {
        if (partyName == null || partyName.isEmpty()) {
            return FigureParty.OTHER;
        }

        try {
            return switch (partyName.trim()) {
                case "더불어민주당" -> FigureParty.DEMOCRATIC;
                case "국민의힘" -> FigureParty.PEOPLE_POWER;
                case "정의당" -> FigureParty.JUSTICE;
                case "국민의당" -> FigureParty.PEOPLES;
                case "기본소득당" -> FigureParty.BASIC_INCOME;
                case "시대전환" -> FigureParty.TIME_TRANSITION;
                case "조국혁신당" -> FigureParty.REBUILDING_KOR;
                case "무소속" -> FigureParty.INDEPENDENT;
                default -> FigureParty.OTHER;
            };
        } catch (Exception e) {
            log.warn("정당명 변환 실패: {}", partyName);
            return null; // 또는 기본값 반환
        }
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


    /**
     * JsonNode에서 텍스트 값 추출
     */
    public String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }

    /**
     * 교육 정보 업데이트
     */
    private void updateEducation(FigureInfoDTO figure) {
        try {
            if (figure.education() == null || figure.education().isEmpty()) {
                return;
            }

            Figure entity = figureRepository.findByFigureIdWithEducation(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));

            // 기존 교육 정보 비우고 새로 추가
            entity.getEducation().clear();
            entity.getEducation().addAll(figure.education());

            figureRepository.save(entity);
            log.debug("교육 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("교육 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
            // 다음 컬렉션 처리를 위해 예외를 던지지 않음
        }
    }

    /**
     * 경력 정보 업데이트
     */
    private void updateCareers(FigureInfoDTO figure) {
        try {
            Figure entity = figureRepository.findByFigureIdWithCareers(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));

            // 컬렉션이 null인 경우 대비
            if (entity.getCareers() == null) {
                entity.setCareers(new ArrayList<>());
            } else {
                entity.getCareers().clear();
            }

            // 새 경력 정보 목록 생성
            List<Career> newCareers = new ArrayList<>();

            // DTO의 경력 정보 추가 (타입 안전하게 처리)
            if (figure.career() != null && !figure.career().isEmpty()) {
                try {
                    // Object 타입이므로 안전하게 변환
                    for (Object careerObj : figure.career()) {
                        if (careerObj instanceof Career) {
                            Career career = (Career) careerObj;
                            newCareers.add(Career.builder()
                                    .title(career.getTitle())
                                    .position(career.getPosition())
                                    .organization(career.getOrganization())
                                    .period(career.getPeriod())
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    log.warn("경력 정보 변환 중 오류: {}", e.getMessage());
                    // 변환 오류가 있어도 계속 진행
                }
            }

            // 국회의원 기본 경력 정보 추가
            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                Career career = Career.builder()
                        .title(figure.electedCount() + "대 국회의원")
                        .position("국회의원")
                        .organization("대한민국 국회")
                        .period(figure.electedDate() != null ? figure.electedDate() + " ~ 현재" : "")
                        .build();
                entity.getCareers().add(career);
            }

            // 위원회 경력 정보 추가
            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                Career career = Career.builder()
                        .title("국회 " + figure.committeeName())
                        .position(position)
                        .organization(figure.committeeName())
                        .period("현재")
                        .build();
                newCareers.add(career);
            }

            entity.getCareers().addAll(newCareers);

            figureRepository.save(entity);
            log.debug("경력 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("경력 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
        }
    }

    /**
     * 사이트 정보 업데이트
     */
    private void updateSites(FigureInfoDTO figure) {
        try {
            Figure entity = figureRepository.findByFigureIdWithSites(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));

            // 기존 사이트 정보 비우기
            entity.getSites().clear();

            // 새 사이트 정보 추가
            if (figure.homepage() != null && !figure.homepage().trim().isEmpty()) {
                entity.getSites().add(figure.homepage().trim());
            }

            if (figure.email() != null && !figure.email().trim().isEmpty()) {
                entity.getSites().add("mailto:" + figure.email().trim());
            }

            figureRepository.save(entity);
            log.debug("사이트 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("사이트 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
        }
    }

    /**
     * 활동 정보 업데이트
     */
    private void updateActivities(FigureInfoDTO figure) {
        try {
            Figure entity = figureRepository.findByFigureIdWithActivities(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));


            // 컬렉션이 null인 경우 대비
            if (entity.getSites() == null) {
                entity.setSites(new ArrayList<>());
            } else {
                entity.getSites().clear();
            }

            // 새 활동 정보 목록 생성
            List<String> newActivities = new ArrayList<>();

            // 당선 정보 추가
            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                newActivities.add(figure.electedCount() + "대 국회의원");
            }

            // 위원회 정보 추가
            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                newActivities.add(figure.committeeName() + " " + position);
            }

            // 컬렉션에 추가
            entity.getActivities().addAll(newActivities);

            // 저장
            figureRepository.save(entity);

            log.debug("활동 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("활동 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
        }
    }
}


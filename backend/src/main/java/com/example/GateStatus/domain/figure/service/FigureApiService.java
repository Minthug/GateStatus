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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
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
    @Transactional
    public int syncAllFigureV2() {
        log.info("모든 국회의원 정보를 동기화 합니다");

        List<FigureInfoDTO> allFigures = fetchAllFiguresFromAPiV2();

        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return 0;
        }

        log.info("동기화 대상 국회의원: {} 명 ", allFigures.size());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int successCount = 0;
        int failCount = 0;

        for (FigureInfoDTO figure : allFigures) {
            try {
                Boolean result = transactionTemplate.execute(status -> {
                    try {
                        Figure figureEntity = figureRepository.findByFigureId(figure.figureId())
                                .orElseGet(() -> Figure.builder()
                                        .figureId(figure.figureId())
                                        .name(figure.name())
                                        .figureType(FigureType.POLITICIAN)
                                        .viewCount(0L)
                                        .build());

                        figureMapper.updateFigureFromDTO(figureEntity, figure);

                        Figure savedFigure = figureRepository.saveAndFlush(figureEntity);

                        // 저장 확인
                        if (savedFigure.getId() == null) {
                            log.error("국회의원 저장 실패 (ID 없음): {}", figure.name());
                            return false;
                        }

                        // 캐시 업데이트
                        figureCacheService.updateFigureCache(savedFigure);

                        log.info("국회의원 저장 성공: {}, ID={}, Entity ID={}",
                                figure.name(), figure.figureId(), savedFigure.getId());

                        // 저장 후 검증
                        Figure verifyFigure = figureRepository.findByFigureId(figure.figureId()).orElse(null);
                        if (verifyFigure == null) {
                            log.error("국회의원 저장 검증 실패: {}, ID={}", figure.name(), figure.figureId());
                            return false;
                        }

                        log.debug("저장 검증 성공: {}, ID={}", figure.name(), figure.figureId());
                        return true;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        log.error("국회의원 저장 오류: {} - {}", figure.name(), e.getMessage(), e);
                        return false;
                    }
                });

                if (Boolean.TRUE.equals(result)) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("트랜잭션 오류: {} - {}", figure.name(), e.getMessage(), e);
                failCount++;
            }
        }
        log.info("국회의원 정보 동기화 완료: 총 {} 명 중 {} 명 성공, {} 명 실패", allFigures.size(), successCount, failCount);
        return successCount;
    }

    @Transactional
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
                // 각 국회의원을 별도 트랜잭션으로 처리
                Boolean result = transactionTemplate.execute(status -> {
                    try {
                        // 1단계: 기본 정보 저장
                        boolean basicSaved = saveBasicFigureInfo(figure);
                        if (!basicSaved) {
                            return false;
                        }

                        // 2단계: 컬렉션 정보 개별 저장
                        updateEducation(figure);
                        updateCareers(figure);
                        updateSites(figure);
                        updateActivities(figure);

                        return true;
                    } catch (Exception e) {
                        log.error("국회의원 저장 중 오류: {} - {}", figure.name(), e.getMessage());
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
            figureRepository.save(figure);

            log.info("국회의원 저장 성공: {}, ID={}", figureDTO.name(), figureDTO.figureId());
            return true;
        } catch (Exception e) {
            log.error("국회의원 저장 실패: {} - {}", figureDTO.name(), e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외 다시 던지기
        }
    }
//
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void updateFigureCollections(FigureInfoDTO infoDTO) {
//        try {
//            log.info("국회의원 컬렉션 정보 업데이트 시작: {}", infoDTO.name());
//
//            Figure figure = figureRepository.findByFigureId(infoDTO.figureId())
//                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다 " + infoDTO.figureId()));
//
//            // 1. 교육 정보 업데이트
//            updateEducation(figure, infoDTO);
//            figureRepository.save(figure);
//            figureRepository.flush();
//
//            // 2. 경력 정보 업데이트
//            updateCareers(figure, infoDTO);
//            figureRepository.save(figure);
//            figureRepository.flush();
//
//            // 3. 사이트 정보 업데이트
//            updateSites(figure, infoDTO);
//            figureRepository.save(figure);
//            figureRepository.flush();
//
//            // 4. 활동 정보 업데이트
//            updateActivities(figure, infoDTO);
//            figureRepository.save(figure);
//            figureRepository.flush();
//
//            log.info("국회의원 컬렉션 정보 업데이트: {}", infoDTO.name());
//        } catch (Exception e) {
//            throw e;
//        }
//    }

    /**
     * 모든 국회의원 정보를 API에서 가져옵니다
     *
     * @return
     */
    private List<FigureInfoDTO> fetchAllFiguresFromAPiV2() {
        log.info("전체 국회의원 정보 API 호출 시작");

        try {
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
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

            if (!rowsNode.isArray()) {
                log.warn("JSON 응답에서 row 배열을 찾을 수 없습니다");
                return Collections.emptyList();
            }

            // 매퍼를 사용하여 DTO로 변환
            List<FigureInfoDTO> figures = figureMapper.mapFromJsonNode(rowsNode);
            log.info("국회의원 정보 파싱 완료: {}명", figures.size());

            return figures;
        } catch (Exception e) {
            log.error("전체 국회의원 정보 조회 중 오류: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
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

            // 기존 경력 정보 비우기
            entity.getCareers().clear();

            // 기존 경력 정보 추가
            if (figure.career() != null && !figure.career().isEmpty()) {
                if (figure.career().get(0) instanceof Career) {
                    entity.getCareers().addAll((List<Career>)figure.career());
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
                entity.getCareers().add(career);
            }

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

            // 기존 활동 정보 비우기
            entity.getActivities().clear();

            // 새 활동 정보 추가
            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                entity.getActivities().add(figure.electedCount() + "대 국회의원");
            }

            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                entity.getActivities().add(figure.committeeName() + " " + position);
            }

            figureRepository.save(entity);
            log.debug("활동 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("활동 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
        }
    }
}


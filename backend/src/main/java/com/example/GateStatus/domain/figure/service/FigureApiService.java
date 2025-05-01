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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
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

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apiKey;

    @Value("${spring.openapi.assembly.figure-api-path}")
    private String figureApiPath;
    @Autowired
    private FigureTransactionService figureTransactionService;


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
    public int syncAllFigureV2() {
        log.info("모든 국회의원 정보를 동기화 합니다");

        List<FigureInfoDTO> allFigures = fetchAllFiguresFromAPiV2();

        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return 0;
        }

        log.info("동기화 대상 국회의원: {} 명 ", allFigures.size());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
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
                        figureRepository.save(figureEntity);
                        log.info("국회의원 저장 성공: {}", figure.name());
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

//    private boolean processSingleFigure(FigureInfoDTO dto) {
//        log.debug("국회의원 정보 처리 시작: ID={}, NAME={}", dto.figureId(), dto.name());
//
//        try {
//            Figure figure = findOrCreateFigure(dto);
//
//            try {
//                figureMapper.updateFigureFromDTO(figure, dto);
//            } catch (Exception e) {
//                log.error("국회의원 정보 업데이트 중 오류: {} - {}", dto.name(), e.getMessage());
//                return false;
//            }
//
//            try {
//                Figure savedFigure = figureRepository.saveAndFlush(figure);
//                log.info("국회의원 저장 성공: ID={}, NAME={}", savedFigure.getName(), savedFigure.getFigureId());
//
//                // 저장 확인 (선택적)
//                boolean exists = figureRepository.existsByFigureId(savedFigure.getFigureId());
//                log.debug("저장 확인: 존재={}, ID={}", exists, savedFigure.getFigureId());
//
//                return true;
//            } catch (Exception e) {
//                log.error("국회의원 저장 중 오류: {} - {}", dto.name(), e.getMessage(), e);
//                return false;
//            }
//        } catch (Exception e) {
//            log.error("국회의원 정보 처리 중 예상치 못한 오류 발생: {} - {}", dto.name(), e.getMessage(), e);
//            return false;
//        }
//    }

//    /**
//     * 국회의원 조회 또는 새 객체 생성
//     * @param info
//     * @return
//     */
//    private Figure findOrCreateFigure(FigureInfoDTO info) {
//        return figureRepository.findByFigureId(info.figureId())
//                .orElseGet(() -> {
//                    log.info("새 국회의원 생성: {}", info.name());
//                    return Figure.builder()
//                            .figureId(info.figureId())
//                            .name(info.name())
//                            .figureType(FigureType.POLITICIAN)
//                            .viewCount(0L)
//                            .build();
//                });
//    }
//
//    /**
//     * 국회의원 정보 로깅
//     * @param figure
//     */
//    private void logSampleFigure(FigureInfoDTO figure) {
//        log.info("샘플 국회의원 정보: ID={}, 이름={}, 정당={}", figure.figureId(), figure.name(),
//                figure.partyName() != null ? figure.partyName() : "없음");
//
//    }
//
//    /**
//     * 모든 국회의원 정보를 API에서 가져옵니다
//     *
//     * @return
//     */
//    private List<FigureInfoDTO> fetchAllFiguresFromApi() {
//        try {
//            log.info("전체 국회의원 정보 API 호출 시작");
//
//            try {
//                String jsonResponse = webClient.get()
//                        .uri(uriBuilder -> uriBuilder
//                                .path(figureApiPath)
//                                .queryParam("key", apiKey)
//                                .build())
//                        .retrieve()
//                        .bodyToMono(String.class)
//                        .block();
//
//                if (jsonResponse == null || jsonResponse.isEmpty()) {
//                    log.error("API에서 빈 응답을 반환했습니다");
//                    throw new ApiDataRetrievalException("API에서 데이터를 가져오지 못했습니다");
//                }
//
//                log.info("API 응답 수신(일부): {}",
//                        jsonResponse.substring(0, Math.min(100, jsonResponse.length())));
//
//                return parseJsonResponse(jsonResponse);
//            } catch (Exception e) {
//                log.error("API 호출 자체에서 오류 발생: {}", e.getMessage(), e);
//                throw new ApiDataRetrievalException("API 호출 실패: " + e.getMessage());
//            }
//        } catch (Exception e) {
//            log.error("전체 API 처리 과정에서 오류 발생: {}", e.getMessage(), e);
//            throw new ApiDataRetrievalException("전체 국회의원 정보를 가져오는 중 오류 발생");
//        }
//    }

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

            return parseJsonResponse(jsonResponse);
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
}


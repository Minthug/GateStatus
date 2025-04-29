package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.domain.figure.service.response.FigureMapper;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureApiService {

    private final WebClient webClient;
    private final FigureApiMapper apiMapper;
    private final FigureRepository figureRepository;
    private final FigureMapper figureMapper;


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
        try {
            log.info("국회의원 정보 API 호출 시작: {}", figureName);

            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("HG_NM", figureName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {
                    })
                    .block();

            log.info("API 응답 데이터:", apiResponse);
            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);

            log.info("전체 국회의원 정보 API 호출 완료: {}명", figures.size());

            if (figures.isEmpty()) {
                return null;
            }

            return figures.get(0);
        } catch (Exception e) {
            throw new ApiDataRetrievalException("국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    /**
     * 모든 국회의원 정보를 동기화 합니다
     *
     * @return
     */
    @Transactional
    public int syncAllFigures() {
        try {
            log.info("모든 국회의원 정보 동기화 시작");

            // API 호출 부분 시도
            List<FigureInfoDTO> allFigures;
            try {
                allFigures = fetchAllFiguresFromApi();
                log.info("API에서 가져온 국회의원 수: {}", allFigures.size());
            } catch (Exception e) {
                log.error("API 호출 중 오류 발생: {}", e.getMessage(), e);
                throw new ApiDataRetrievalException("API에서 국회의원 정보를 가져오는 중 오류 발생: " + e.getMessage());
            }

            // 확인을 위해 첫 번째 DTO 로깅
            if (!allFigures.isEmpty()) {
                log.info("첫 번째 국회의원 정보: {}", allFigures.get(0));
            } else {
                log.warn("API에서 가져온 국회의원 정보가 없습니다");
                return 0;
            }

            int count = 0;
            for (FigureInfoDTO infoDTO : allFigures) {
                try {
                    log.info("동기화 시도 ID: {}, 이름: {}", infoDTO.figureId(), infoDTO.name());

                    Figure figure = figureRepository.findByFigureId(infoDTO.figureId())
                            .orElseGet(() -> {
                                log.info("새 국회의원 생성: {}", infoDTO.name());
                                log.info("figureId: {}", infoDTO.figureId());
                                return Figure.builder()
                                        .figureId(infoDTO.figureId())
                                        .name(infoDTO.name())
                                        .figureType(FigureType.POLITICIAN)
                                        .viewCount(0L)
                                        .build();
                            });

                    // 매퍼 호출 부분 try-catch로 감싸기
                    try {
                        figureMapper.updateFigureFromDTO(figure, infoDTO);
                    } catch (Exception e) {
                        log.error("매퍼 처리 중 오류 발생: {} - {}", infoDTO.name(), e.getMessage(), e);
                        continue; // 이 국회의원은 건너뛰고 다음으로 진행
                    }

                    // 저장 시도 부분
                    try {
                        figureRepository.save(figure);
                        count++;
                        log.info("국회의원 저장 성공: {}", infoDTO.name());
                    } catch (Exception e) {
                        log.error("국회의원 저장 중 오류 발생: {} - {}", infoDTO.name(), e.getMessage(), e);
                    }
                } catch (Exception e) {
                    log.error("국회의원 처리 중 전체 오류: {} - {}", infoDTO.name(), e.getMessage(), e);
                }
            }

            log.info("국회의원 정보 동기화 완료: {}명 중 {}명 성공", allFigures.size(), count);
            return count;
        } catch (Exception e) {
            log.error("전체 국회의원 동기화 중 오류 발생: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("전체 국회의원 정보를 동기화 하는 중 오류 발생");
        }
    }

    /**
     * 모든 국회의원 정보를 API에서 가져옵니다
     *
     * @return
     */
    private List<FigureInfoDTO> fetchAllFiguresFromApi() {
        try {
            log.info("전체 국회의원 정보 API 호출 시작");

            try {
                String jsonResponse = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(figureApiPath)
                                .queryParam("key", apiKey)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (jsonResponse == null || jsonResponse.isEmpty()) {
                    log.error("API에서 빈 응답을 반환했습니다");
                    throw new ApiDataRetrievalException("API에서 데이터를 가져오지 못했습니다");
                }

                log.info("API 응답 수신(일부): {}",
                        jsonResponse.substring(0, Math.min(100, jsonResponse.length())));

                return parseJsonResponse(jsonResponse);
            } catch (Exception e) {
                log.error("API 호출 자체에서 오류 발생: {}", e.getMessage(), e);
                throw new ApiDataRetrievalException("API 호출 실패: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("전체 API 처리 과정에서 오류 발생: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("전체 국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    private List<FigureInfoDTO> parseJsonResponse(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);

            JsonNode rowsNode = rootNode.path("nwvrqwxyaytdsfvhu")
                    .path(1)
                    .path("row");

            if (!rowsNode.isArray()) {
                log.warn("JSON 응답에서 row 배열을 찾을 수 없습니다: {}",
                        rootNode.path("nwvrqwxyaytdsfvhu").toString().substring(0, 100));
                return Collections.emptyList();
            }

            List<FigureInfoDTO> result = new ArrayList<>();

            for (JsonNode row : rowsNode) {
                try {
                    String figureId = apiMapper.getTextValue(row, "MONA_CD");
                    String name = apiMapper.getTextValue(row, "HG_NM");
                    String englishName = apiMapper.getTextValue(row, "ENG_NM");
                    String birth = apiMapper.getTextValue(row, "BTH_DATE");
                    String partyNameStr = apiMapper.getTextValue(row, "POLY_NM");
                    String constituency = apiMapper.getTextValue(row, "ORIG_NM");
                    String committeeName = apiMapper.getTextValue(row, "CMIT_NM");
                    String committeePosition = apiMapper.getTextValue(row, "JOB_RES_NM");
                    String electedCount = apiMapper.getTextValue(row, "REELE_GBN_NM");
                    String electedDate = apiMapper.getTextValue(row, "UNITS");
                    String reelection = apiMapper.getTextValue(row, "REELE_GBN_NM");
                    String email = apiMapper.getTextValue(row, "E_MAIL");
                    String homepage = apiMapper.getTextValue(row, "HOMEPAGE");


                    // 경력 정보
                    String careerText = apiMapper.getTextValue(row, "MEM_TITLE");
                    List<String> career = new ArrayList<>();
                    if (careerText != null && !careerText.isEmpty()) {
                        String[] lines = careerText.split("\r\n");
                        for (String line : lines) {
                            if (line != null && !line.trim().isEmpty()) {
                                career.add(line.trim());
                            }
                        }
                    }
                    // 기타 정보 - 실제 응답에 없는 경우 null 또는 빈 리스트로
                    List<String> education = new ArrayList<>();
                    String profileUrl = null;
                    String blog = null;
                    String facebook = null;


                    // 정당명 변환
                    FigureParty partyName = convertToFigureParty(partyNameStr);

                    FigureInfoDTO dto = new FigureInfoDTO(
                            figureId, name, englishName, birth, partyName, constituency,
                            committeeName, committeePosition, electedCount, electedDate,
                            reelection, profileUrl, education, career,
                            email, homepage, blog, facebook
                    );

                    result.add(dto);
                    log.debug("의원 정보 파싱 성공: {}", name);
                } catch (Exception e) {
                    log.warn("국회의원 정보 파싱 중 오류: {}", e.getMessage());
                }
            }


            log.info("국회의원 정보 파싱 완료: {} 명", result.size());
            return result;
        } catch (Exception e) {
            log.error("JSON 파싱 중 오류 발생: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("JSON 파싱 실패: " + e.getMessage());
        }
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

            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("POLY_NM", partyName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {
                    })
                    .block();

            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);
            log.info("{}당 소속 국회의원 정보 API 호출 완료: {}명", partyName, figures.size());

            return figures;
        } catch (Exception e) {

            log.error("정당별 국회의원 정보 조회 중 오류 발생: {}", partyName, e);
            throw new ApiDataRetrievalException("정당별 국회의원 정보를 가져오는 중 오류 발생");
        }
    }


    // 문자열을 구분자로 분리하여 리스트에 추가
    private void splitAndAddToList(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            // 여러 가능한 구분자로 시도 (세미콜론, 쉼표, 줄바꿈 등)
            String[] items = value.split("[;,\n]+");
            for (String item : items) {
                if (item != null && !item.trim().isEmpty()) {
                    list.add(item.trim());
                }
            }
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
                case "무소속" -> FigureParty.INDEPENDENT;
                default -> FigureParty.OTHER;
            };
        } catch (Exception e) {
        log.warn("정당명 변환 실패: {}", partyName);
        return null; // 또는 기본값 반환
        }
    }

}

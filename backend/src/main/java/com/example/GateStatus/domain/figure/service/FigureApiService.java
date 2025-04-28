package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.domain.figure.service.response.FigureMapper;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

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
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
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
     * @return
     */
//    @Transactional
//    public int syncAllFigures() {
//        try {
//            log.info("모든 국회의원 정보 동기화 시작");
//            List<FigureInfoDTO> allFigures = fetchAllFiguresFromApi();
//            log.info("가져온 국회의원 수: {}", allFigures.size());
//
//            int count = 0;
//
//            for (FigureInfoDTO infoDTO : allFigures) {
//                try {
//                    Figure figure = figureRepository.findByName(infoDTO.name())
//                            .orElseGet(() -> Figure.builder()
//                                    .name(infoDTO.name())
//                                    .figureType(FigureType.POLITICIAN)
//                                    .viewCount(0L)
//                                    .build());
//
//                    figureMapper.updateFigureFromDTO(figure, infoDTO);
//                    figureRepository.save(figure);
//                    count++;
//                } catch (Exception e) {
//                    log.error("국회의원 동기화 중 오류 발생: {} - {}", infoDTO.name(), e.getMessage(), e);
//                }
//            }
//            log.info("국회의원 정보 동기화 완료: {}", count);
//            return count;
//        } catch (Exception e) {
//            throw new ApiDataRetrievalException("전체 국회의원 정보를 동기화 하는 중 오류 발생");
//        }
//    }
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
            // 각 국회의원 정보 처리 및 저장
            for (FigureInfoDTO infoDTO : allFigures) {
                try {
                    log.info("국회의원 정보 처리 중: {}", infoDTO.name());

                    Figure figure = figureRepository.findByName(infoDTO.name())
                            .orElseGet(() -> {
                                log.info("새 국회의원 생성: {}", infoDTO.name());
                                return Figure.builder()
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
     * @return
     */
//    private List<FigureInfoDTO> fetchAllFiguresFromApi() {
//        try {
//            log.info("전체 국회의원 정보 API 호출 시작");
//
//            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
//                    .uri(uriBuilder -> uriBuilder
//                            .path(figureApiPath)
//                            .queryParam("key", apiKey)
//                            .build())
//                    .retrieve()
//                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
//                    .block();
//
//            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);
//            log.info("전체 국회의원 정보 API 호출 완료: {}명", figures.size());
//
//            return figures;
//        } catch (Exception e) {
//            throw new ApiDataRetrievalException("전체 국회의원 정보를 가져오는 중 오류 발생");
//        }
//    }
    private List<FigureInfoDTO> fetchAllFiguresFromApi() {
        try {
            log.info("전체 국회의원 정보 API 호출 시작");

            // API 호출 부분 시도
            AssemblyApiResponse<JsonNode> apiResponse;
            try {
                apiResponse = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(figureApiPath)
                                .queryParam("key", apiKey)
                                .build())
                        .accept(MediaType.APPLICATION_XML)
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(xmlResponse -> {
                            return covertXmlToObject(xmlResponse);
                        })
                        .block();

                if (apiResponse != null) {
                    log.info("API 응답 코드: {} ", apiResponse.resultCode());
                    log.info("API 응답 메시지: {}", apiResponse.resultMessage());

                    JsonNode data = apiResponse.data();
                    if (data != null) {
                        log.info("API 데이터 샘플: {}", data.toString().substring(0, Math.min(500, data.toString().length())));
                    } else {
                        log.info("API 데이터가 null입니다");
                    }
                } else {
                    log.error("API 응답이 NULL 입니다");
                }

            } catch (Exception e) {
                log.error("API 호출 자체에서 오류 발생: {}", e.getMessage(), e);
                throw new ApiDataRetrievalException("API 호출 실패: " + e.getMessage());
            }

            // 응답 처리 부분
            try {
                if (apiResponse == null) {
                    log.error("API 응답이 NULL입니다");
                    throw new ApiDataRetrievalException("API 응답이 NULL입니다");
                }

                // 응답 내용 일부 로깅 (JSON이 너무 크면 일부만)
                JsonNode data = apiResponse.data();
                if (data != null) {
                    if (data.isArray() && data.size() > 0) {
                        log.info("API 응답 첫 번째 항목 샘플: {}", data.get(0).toString());
                        log.info("API 응답 항목 개수: {}", data.size());
                    } else {
                        log.info("API 응답 데이터: {}", data.toString().substring(0, Math.min(500, data.toString().length())));
                    }
                } else {
                    log.error("API 응답 데이터가 NULL입니다");
                    throw new ApiDataRetrievalException("API 응답 데이터가 NULL입니다");
                }

                List<FigureInfoDTO> figures;
                try {
                    figures = apiMapper.map(apiResponse);
                    log.info("매핑된 국회의원 수: {}", figures.size());
                    if (!figures.isEmpty()) {
                        log.info("첫 번째 매핑 결과 샘플: {}", figures.get(0));
                    }
                } catch (Exception e) {
                    log.error("API 응답 매핑 중 오류 발생: {}", e.getMessage(), e);
                    throw new ApiDataRetrievalException("API 응답 매핑 실패: " + e.getMessage());
                }

                return figures;
            } catch (Exception e) {
                log.error("API 응답 처리 중 오류 발생: {}", e.getMessage(), e);
                throw new ApiDataRetrievalException("API 응답 처리 실패: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("전체 API 처리 과정에서 오류 발생: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("전체 국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    /**
     * 특정 정당 소속 국회의원 정보를 동기화합니다
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
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();

            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);
            log.info("{}당 소속 국회의원 정보 API 호출 완료: {}명", partyName, figures.size());

            return figures;
        } catch (Exception e) {

            log.error("정당별 국회의원 정보 조회 중 오류 발생: {}", partyName, e);
            throw new ApiDataRetrievalException("정당별 국회의원 정보를 가져오는 중 오류 발생");
        }
    }
}

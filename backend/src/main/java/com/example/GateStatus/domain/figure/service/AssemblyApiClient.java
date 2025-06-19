package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.example.GateStatus.domain.common.JsonUtils.isEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssemblyApiClient {

    private final WebClient assemblyWebClient;
    private final ObjectMapper objectMapper;
    private final FigureMapper figureMapper;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apiKey;

    @Value("${spring.openapi.assembly.figure-api-path}")
    private String figureApiPath;

    public FigureInfoDTO fetchFigureByName(String figureName) {
        log.info("국회의원 정보 API 호출: {}", figureName);

        try {
            String jsonResponse = assemblyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("HG_NM", figureName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (isEmpty(jsonResponse)) {
                log.warn("API 응답 없음: {}", figureName);
                return null;
            }

            List<FigureInfoDTO> figures = parseApiResponse(jsonResponse);
            return figures.isEmpty() ? null : figures.get(0);
        } catch (Exception e) {
            log.error("API 호출 실패: {} - {}", figureName, e.getMessage(), e);
            throw new ApiDataRetrievalException("API 호출 실패: " + e.getMessage());
        }
    }

    public List<FigureInfoDTO> fetchAllFigures() {

    }

    public List<FigureInfoDTO> fetchFiguresByParty(String partyName) {

    }

    private List<FigureInfoDTO> fetchFiguresPage(int pageNo, int pageSize) {

    }


    private List<FigureInfoDTO> parseApiResponse(String jsonResponse) {
        return null;
    }

}

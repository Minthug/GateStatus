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


    @Value("${openapi.assembly.url}")
    private String baseUrl;

    @Value("${openapi.assembly.key}")
    private String apiKey;

    @Value("${openapi.assembly.member-api-path}")
    private String memberApiPath;


    @Transactional
    public Figure syncFigureFromApi(String figureName) {
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
                            .path("/nwvrqwxyaytdsfvhu")
                            .queryParam("KEY", apiKey)
                            .queryParam("HG_NM", figureName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();

            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);

            if (figures.isEmpty()) {
                return null;
            }

            return figures.get(0);
        } catch (Exception e) {
            throw new ApiDataRetrievalException("국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    @Transactional
    public int syncAllFigures() {
        try {
            log.info("모든 국회의원 정보 동기화 시작");
            List<FigureInfoDTO> allFigures = fetchAllFiguresFromApi();
            int count = 0;

            for (FigureInfoDTO infoDTO : allFigures) {
                try {
                    Figure figure = figureRepository.findByName(infoDTO.name())
                            .orElseGet(() -> Figure.builder()
                                    .name(infoDTO.name())
                                    .figureType(FigureType.POLITICIAN)
                                    .viewCount(0L)
                                    .build());

                    figureMapper.updateFigureFromDTO(figure, infoDTO);
                    figureRepository.save(figure);
                    count++;
                } catch (Exception e) {
                    log.error("국회의원 동기화 중 오류 발생: {}", infoDTO.name());
                }
            }
            log.info("국회의원 정보 동기화 완료: {}", count);
            return count;
        } catch (Exception e) {
            throw new ApiDataRetrievalException("전체 국회의원 정보를 동기화 하는 중 오류 발생");
        }
    }

    private List<FigureInfoDTO> fetchAllFiguresFromApi() {
        try {
            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("nwvrqwxyaytdsfvhu")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();
            return apiMapper.map(apiResponse);
        } catch (Exception e) {
            throw new ApiDataRetrievalException("전체 국회의원 정보를 가져오는 중 오류 발생");
        }
    }
}

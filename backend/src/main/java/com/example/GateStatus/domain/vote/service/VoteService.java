package com.example.GateStatus.domain.vote.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.global.config.exception.ApiClientException;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.example.GateStatus.global.config.exception.ApiServerException;
import com.example.GateStatus.global.config.open.ApiResponseMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.example.GateStatus.global.config.open.RedisCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static com.example.GateStatus.domain.vote.service.BillVoteDTO.getTextValue;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final WebClient webClient;
    private final FigureRepository figureRepository;
    private final RedisCacheService cacheService;
    private final ApiResponseMapper mapper;
    private final ApiResponseMapper apiResponseMapper;

    @Value("${spring.openapi.assembly.url}")
    private String voteApiUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apiKey;

    public List<BillVoteDTO> getVotesByFigureId(Long figureId) {
        String cacheKey = "votes:figure:" + figureId;
        return cacheService.getOrSet(cacheKey, () -> fetchVotesFromApi(figureId), 86400);
    }

    private List<BillVoteDTO> fetchVotesFromApi(Long figureId) {
        try {
            Figure figure = figureRepository.findById(figureId)
                    .orElseThrow(() -> new EntityNotFoundException("정치인을 찾을 수 없습니다"));

            log.info("국회 API 호출 시작: 정치인= {}", figure.getName());

            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ncocpgfiaoituanbr")
                            .queryParam("KEY", apiKey)
                            .queryParam("AGE", "21")
                            .queryParam("PROPOSER", figure.getName())
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            Mono.error(new ApiClientException("API 클라이언트 오류: " + response.statusCode())))
                    .onStatus(status -> status.is5xxServerError(), response ->
                            Mono.error(new ApiServerException("API 서버 오류: " + response.statusCode())))
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();

            if (apiResponse == null) {
                log.warn("API 응답이 null 입니다. 빈 결과 반환");
                return Collections.emptyList();
            }

            List<BillVoteDTO> votes = mapper.mapToBillVoteDTOs(apiResponse);
            log.info("API 호출 완료: 정치인= {}, 결과 수= {}", figure.getName(), votes.size());
            return votes;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("투표 데이터 조회 중 오류 발생", e);
            throw new ApiDataRetrievalException("투표 데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public BillDetailDTO getBillDetail(String billNo) {
        String cacheKey = "bill:detail:" + billNo;

        return cacheService.getOrSet(cacheKey, () -> fetchBillDetailFromApi(billNo), 86400);
    }

    private BillDetailDTO fetchBillDetailFromApi(String billNo) {
        try {
            log.info("법안 상세 정보 API 호출 시작: 법안번호 = {}", billNo);

            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ncocndinfopromain")
                            .queryParam("KEY", apiKey)
                            .queryParam("BILL_ID", billNo)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        return Mono.error(new ApiClientException("Api 클라이언트 오류: " + response.statusCode()));
                    })
                    .onStatus(status -> status.is5xxServerError(), response -> {
                        return Mono.error(new ApiServerException("Api 서버 오류: " + response.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();

            BillDetailDTO result = apiResponseMapper.mapToBillDetailDTO(apiResponse, billNo);

            log.info("법안 상세 정보 API 호출 완료: {}", billNo);
            return result;

        } catch (ApiClientException | ApiServerException e) {
            log.error("법안 상세 정보 API 호출 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("법안 상세 정보 조회 중 예외 발생", e);
            throw new ApiDataRetrievalException("법안 상세 정보를 가져오는 중 오류 발생: ");
        }
    }

    private VoteResultDetail parseVoteResults(JsonNode billInfo) {
        try {
            int agreeCount = getIntValue(billInfo, "AGREE_COUNT");
            int disagreeCount = getIntValue(billInfo, "DISAGREE_COUNT");
            int abstainCount = getIntValue(billInfo, "ABSTAIN_COUNT");
            int absentCount = getIntValue(billInfo, "ABSENT_COUNT");

            return new VoteResultDetail(
                    agreeCount,
                    disagreeCount,
                    abstainCount,
                    absentCount,
                    agreeCount + disagreeCount + abstainCount + absentCount
            );
    } catch (Exception e) {
            log.warn("투표 결과 파싱 중 오류 발생: ", e);
            return new VoteResultDetail(0, 0, 0, 0, 0);
        }
}

    private int getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return 0;
        }

        try {
            return field.asInt();
        } catch (Exception e) {
            return 0;
        }
    }
}


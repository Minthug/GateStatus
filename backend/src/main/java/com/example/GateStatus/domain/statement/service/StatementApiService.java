package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementApiService {

    private final WebClient.Builder webClientBuilder;
    private final StatementApiMapper apiMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apikey;

    @Value("${spring.openapi.assembly.news-figure-path}")
    private String newsFigurePath;  // 설정 파일의 경로 주입

    /**
     * 정치인 이름으로 발언 검색
     * @param name
     * @return
     */
    public List<StatementResponse> getStatementsByPolitician(String name) {
        log.info("정치인 '{}' 발언 정보 API 조회 시작", name);

        String cacheKey = "statements:politician:" + name;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("캐시 히트: {}", cacheKey);
            return (List<StatementResponse>) cached;
        }
        log.debug("캐시 미스: {}", cacheKey);

        AssemblyApiResponse<String> apiResponse = fetchStatementsByFigure(name);

        if (!apiResponse.isSuccess()) {
            log.warn("API 응답 실패: {}", apiResponse.resultMessage());
            redisTemplate.opsForValue().set(cacheKey, Collections.emptyList(), 30, TimeUnit.SECONDS);
            return Collections.emptyList();
        }

        List<StatementApiDTO> apiDtos = apiMapper.map(apiResponse);

        List<StatementResponse> responses = apiDtos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        if (!responses.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, responses, 1, TimeUnit.HOURS);
            log.debug("캐시 저장 성공 (JSON): {}, 만료 시간: 1시간", cacheKey);
        } else {
            redisTemplate.opsForValue().set(cacheKey, responses, 30, TimeUnit.SECONDS);
            log.debug("빈 결과는 짧은 시간만 캐싱: {}", cacheKey);
        }
        return responses;
    }

    private StatementResponse convertToResponse(StatementApiDTO dto) {

        Map<String, Object> nlpData = new HashMap<>();
        List<String> checkableItems = apiMapper.extractCheckableItems(dto.content());
        if (!checkableItems.isEmpty()) {
            nlpData.put("checkableItems", checkableItems);
        }

        return new StatementResponse(
                null,
                null,
                dto.figureName(),
                dto.title(),
                dto.content(),
                dto.statementDate(),
                dto.source(),
                dto.context(),
                dto.originalUrl(),
                determineStatementType(dto.typeCode()),
                null,
                null,
                checkableItems,
                nlpData,
                0,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    /**
     * API 응답을 파싱하여 발언 목록 반환
     * @param response
     * @return
     */
    private List<StatementApiDTO> parseResponseToStatements(AssemblyApiResponse<String> response) {
        if (response.data() == null) {
            log.warn("API 응답 데이터 없음");
            return Collections.emptyList();
        }

        try {
            List<StatementApiDTO> statements = apiMapper.map(response);

            if (statements.isEmpty()) {
                log.info("검색 결과가 없습니다");
            }

            return statements;
        } catch (Exception e) {
            log.error("응답 파싱 중 오류: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<StatementResponse> getStatementsByKeyword(String keyword) {
        try {
            AssemblyApiResponse<String> response = searchStatementsByKeyword(keyword);
            if (!response.isSuccess()) {
                log.error("API 호출 실패: {}", response.resultMessage());
                return List.of();
            }

            return apiMapper.mapToStatementResponses(response);
        } catch (Exception e) {
            log.error("키워드 검색 중 오류: {}", e.getMessage());
            return List.of();
        }
    }

    private AssemblyApiResponse<String> searchStatementsByKeyword(String keyword) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .build();

        try {
            String xmlResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/nauvppbxargkmyovh")
                            .queryParam("apiKey", apikey)
                            .queryParam("keyword", keyword)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String resultCode = extractResultCode(xmlResponse);
            String resultMessage = extractResultMessage(xmlResponse);

            return new AssemblyApiResponse<>(resultCode, resultMessage, xmlResponse);
        } catch (Exception e) {
            log.error("API 호출 중 오류: {}", e.getMessage(), e);
            return new AssemblyApiResponse<>("99", e.getMessage(), null);
        }
    }

    public List<StatementResponse> searchStatements(String politician, String keyword) {
        if (politician != null) {
            List<StatementResponse> statements = getStatementsByPolitician(politician);

            if (keyword != null && !keyword.isEmpty()) {
                return statements.stream()
                        .filter(stmt ->
                                stmt.title().contains(keyword) || stmt.content().contains(keyword))
                        .collect(Collectors.toList());
            }
            return statements;
        } else if (keyword != null) {
            return getStatementsByKeyword(keyword);
        }

        return List.of();
    }


    public AssemblyApiResponse<String> fetchStatementsByFigure(String figureName) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .build();

        String xmlResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/nauvppbxargkmyovh")
                        .queryParam("apiKey", apikey)
                        .queryParam("name", figureName)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String resultCode = extractResultCode(xmlResponse);
        String resultMessage = extractResultMessage(xmlResponse);

        return new AssemblyApiResponse<>(resultCode, resultMessage, xmlResponse);
    }

    // XML 응답에서 결과 코드/메시지 추출 유틸리티 메소드
    private String extractResultCode(String response) {
        if (response == null) return "ERROR";

        // RESULT 태그 내의 CODE 값 추출
        try {
            int startIdx = response.indexOf("<CODE>");
            if (startIdx != -1) {
                startIdx += "<CODE>".length();
                int endIdx = response.indexOf("</CODE>", startIdx);
                if (endIdx != -1) {
                    return response.substring(startIdx, endIdx);
                }
            }
        } catch (Exception e) {
            log.warn("결과 코드 추출 실패: {}", e.getMessage());
        }

        return "UNKNOWN";
    }

    private String extractResultMessage(String response) {
        if (response == null) return "Unknown error";

        // RESULT 태그 내의 MESSAGE 값 추출
        try {
            int startIdx = response.indexOf("<MESSAGE>");
            if (startIdx != -1) {
                startIdx += "<MESSAGE>".length();
                int endIdx = response.indexOf("</MESSAGE>", startIdx);
                if (endIdx != -1) {
                    return response.substring(startIdx, endIdx);
                }
            }
        } catch (Exception e) {
            log.warn("결과 메시지 추출 실패: {}", e.getMessage());
        }

        return "Unknown message";
    }


    /**
     * API의 발언 유형 코드를 애플리케이션 StatementType으로 변환
     * @param typeCode
     * @return
     */
    public StatementType determineStatementType(String typeCode) {
        switch (typeCode) {
            case "SPEECH":
                return StatementType.SPEECH;
            case "INTERVIEW":
                return StatementType.INTERVIEW;
            case "PRESS":
                return StatementType.PRESS_RELEASE;
            case "DEBATE":
                return StatementType.DEBATE;
            case "ASSEMBLY":
                return StatementType.ASSEMBLY_SPEECH;
            case "COMMITTEE":
                return StatementType.COMMITTEE_SPEECH;
            case "MEDIA":
                return StatementType.MEDIA_COMMENT;
            case "SNS":
                return StatementType.SOCIAL_MEDIA;
            default:
                return StatementType.OTHER;
        }
    }
}

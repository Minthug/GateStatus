package com.example.GateStatus.domain.statement.service;

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

import javax.swing.plaf.nimbus.State;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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

    // ==================== 캐시 관련 상수 ====================

    private static final String CACHE_PREFIX_POLITICIAN = "statements:politician:";
    private static final String CACHE_PREFIX_KEYWORD = "statements:keyword:";
    private static final String CACHE_PREFIX_SEARCH = "statements:search:";
    private static final String CACHE_PREFIX_PERIOD = "statements:period:";

    private static final int CACHE_SUCCESS_HOURS = 1;
    private static final int CACHE_EMPTY_SECONDS = 30;
    private static final int CACHE_ERROR_SECONDS = 10;

    // ==================== 공개 API 메서드들 ====================

    /**
     * 정치인 이름으로 발언 검색
     * @param name
     * @return
     */
    public List<StatementResponse> getStatementsByPolitician(String name) {
        log.info("정치인 '{}' 발언 정보 API 조회 시작", name);

        String cacheKey = CACHE_PREFIX_POLITICIAN + name;

        List<StatementResponse> cachedResult = getCachedStatements(cacheKey);
        if (cachedResult != null) {
            log.debug("캐시 히트: {}", cacheKey);
            return cachedResult;
        }

        log.debug("캐시 미스: {}", cacheKey);

        try {
            AssemblyApiResponse<String> apiResponse = fetchStatementsByFigure(name);

            if (!apiResponse.isSuccess()) {
                log.warn("API 응답 실패: {}", apiResponse.resultMessage());
                cacheEmptyResult(cacheKey);
                return Collections.emptyList();
            }

            List<StatementResponse> responses = apiMapper.mapToStatementResponses(apiResponse);

            cacheStatements(cacheKey, responses);

            log.info("정치인 '{}' 발언 정보 조회 완료: {}건", name, responses.size());
            return responses;
        } catch (Exception e) {
            log.error("정치인 '{}' 발언 조회 중 오류: {}", name, e.getMessage(), e);
            cacheErrorResult(cacheKey);
            return Collections.emptyList();
        }
    }

    public List<StatementResponse> getStatementsByKeyword(String keyword) {
        log.info("키워드 '{}' 발언 검색 시작", keyword);

        String cacheKey = CACHE_PREFIX_KEYWORD + keyword;

        List<StatementResponse> cachedResult = getCachedStatements(cacheKey);
        if (cachedResult != null) {
            log.debug("캐시 히트: {}", cacheKey);

            return cachedResult;
        }

        try {
            AssemblyApiResponse<String> response = searchStatementsByKeyword(keyword);
            if (!response.isSuccess()) {
                log.error("API 호출 실패: {}", response.resultMessage());
                cachedEmptyResult(cacheKey);
                return Collections.emptyList();
            }

            List<StatementResponse> responses = apiMapper.mapToStatementResponses(response);

            cachedStatements(cacheKey, responses);

            log.info("키워드 '{}' 발언 검색 완료: {}건", keyword, responses.size());
            return responses;
        } catch (Exception e) {
            log.error("키워드 검색 중 오류: {}", e.getMessage());
            cacheErrorResult(cacheKey);
            return Collections.emptyList();
        }
    }

    public List<StatementResponse> searchStatements(String politician, String keyword) {
        log.info("복합 검색 시작: 정치인='{}', 키워드='{}'", politician, keyword);

        if ((politician == null || politician.trim().isEmpty()) &&
            (keyword == null) || keyword.trim().isEmpty()) {
            log.warn("정치인과 키워드가 모두 비어있음");
            return Collections.emptyList();
        }

        String cacheKey = CACHE_PREFIX_SEARCH +
                (politician != null ? politician : "") + ":" + (keyword != null ? keyword : "");

        List<StatementResponse> cachedResult = getCachedStatements(cacheKey);
        if (cachedResult != null) {
            log.debug("캐시 히트: {}", cacheKey);
            return cachedResult;
        }

        log.debug("캐시 미스: {}", cacheKey);

        try {
            List<StatementResponse> result = performComplexSearch(politician, keyword);
            cachedStatements(cacheKey, result);
            log.info("복합 검색 완료: 정치인='{}', 키워드='{}', 결과={}건",
                    politician, keyword, result.size());
            return result;
        } catch (Exception e) {
            log.error("복합 검색 중 오류: {}", e.getMessage(), e);
            cacheErrorResult(cacheKey);
            return Collections.emptyList();
        }
    }


    private AssemblyApiResponse<String> searchStatementsByKeyword(String keyword) {
        log.debug("API 호출: 키워드 발언 검색 - {}", keyword);

        WebClient webClient = createWebClient();

        try {
            String xmlResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(newsFigurePath)
                            .queryParam("apiKey", apikey)
                            .queryParam("keyword", keyword)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return new createApiResponse(xmlResponse);
        } catch (Exception e) {
            log.error("API 호출 중 오류: {}", e.getMessage(), e);
            return new AssemblyApiResponse<>("99", e.getMessage(), null);
        }
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

    public AssemblyApiResponse<String> fetchStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
        return null;
    }

    // ==================== 캐시 관련 메서드들 ====================

    @SuppressWarnings("unchecked")
    private List<StatementResponse> getCachedStatements(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<StatementResponse>) cached;
            }
        } catch (Exception e) {
            log.warn("캐시 조회 실패, 캐시 무시: {} - {}", cacheKey, e.getMessage());
        }
        return null;
    }

    private void cacheStatements(String cacheKey, List<StatementResponse> statements) {
        cacheStatements(cacheKey, statements, CACHE_SUCCESS_HOURS);
    }

    private void cacheStatements(String cacheKey, List<StatementResponse> statements, int hours) {
        try {
            if (!statements.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, statements, hours, TimeUnit.HOURS);
                log.debug("캐시 저장 성공: {}, 만료 시간: {}시간", cacheKey, hours);
            } else {
                cacheEmptyResult(cacheKey);
            }
        } catch (Exception e) {
            log.warn("캐시 저장 실패: {} - {}", cacheKey, e.getMessage());
        }
    }

    private void cacheEmptyResult(String cacheKey) {
        try {
            redisTemplate.opsForValue().set(cacheKey, Collections.emptyList(),
                    CACHE_EMPTY_SECONDS, TimeUnit.SECONDS);
            log.debug("빈 결과 캐시: {}, 만료 시간: {}초", cacheKey, CACHE_EMPTY_SECONDS);
        } catch (Exception e) {
            log.warn("빈 결과 캐시 실패: {}", cacheKey);
        }
    }

    private void cacheErrorResult(String cacheKey) {
        try {
            redisTemplate.opsForValue().set(cacheKey, Collections.emptyList(),
                    CACHE_ERROR_SECONDS, TimeUnit.SECONDS);
            log.debug("에러 결과 캐시: {}, 만료 시간: {}초", cacheKey, CACHE_ERROR_SECONDS);
        } catch (Exception e) {
            log.warn("에러 결과 캐시 실패: {}", cacheKey);
        }
    }

    // ==================== XML 파싱 유틸리티 메서드들 ====================


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

}

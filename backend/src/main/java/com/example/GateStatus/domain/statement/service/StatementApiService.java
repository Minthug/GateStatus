package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementApiService {

    private final WebClient.Builder webClientBuilder;
    private final StatementApiMapper apiMapper;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apikey;


    /**
     * 정치인 이름으로 발언 검색
     * @param name
     * @return
     */
    public List<StatementApiDTO> getStatementsByPolitician(String name) {
        log.info("정치인 '{}' 발언 정보 API 조회 시작", name);

        try {
            log.info("API 호출 URL: {}, 매개변수: name={}",
                    baseUrl + "/news/figure", name,
                    apikey.substring(0, Math.min(4, apikey.length())) + "***");

            WebClient webClient = webClientBuilder.baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                    .build();

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/news/figure")
                            .queryParam("apiKey", apikey)
                            .queryParam("name", name)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                log.info("API 응답 수신 - 길이: {} 바이트", response.length());

                String previewContent = response.substring(0, Math.min(500, response.length()));
                log.debug("API 응답 미리보기: {}", previewContent);

                if (response.contains("nauvppbxargkmyovh")) {
                    log.info("응답에 'nauvppbxargkmyovh' 포함됨 - 국회 API 응답 헤더로 보임");
                }

                if (response.contains("<CODE>")) {
                    String resultCode = extractResultCode(response);
                    String resultMessage = extractResultMessage(resultCode);

                    log.info("API 응답 결과 코드: {}, 메시지: {}", resultCode, resultMessage);

                    if (!"INFO-000".equals(resultCode)) {
                        log.warn("API 응답이 INFO-000이 아님. 오류 가능성: {}", resultMessage);
                    }
                }

                if (response.contains("<row>")) {
                    log.info("API 응답에 데이터 행(<row>) 포함됨");
                } else {
                    log.warn("API 응답에 데이터 행(<row>)이 없음. 데이터 없음 또는 형식 불일치");
                }
            } else {
                log.warn("API가 null 응답 반환");
                return Collections.emptyList();
            }

            AssemblyApiResponse<String> apiResponse = new AssemblyApiResponse<>(
                    extractResultCode(response),
                    extractResultMessage(response),
                    response);

            if (!apiResponse.isSuccess()) {
                log.warn("API 응답 실패: {}", apiResponse.resultMessage());
                return Collections.emptyList();
            }

            List<StatementApiDTO> result = apiMapper.map(apiResponse);
            log.info("API 결과 처리 완료: {}건의 발언 정보", result.size());

            return result;
        } catch (Exception e) {
            log.error("API 호출 중 오류 발생: {}", e.getMessage(), e);

            return Collections.emptyList();
        }
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

    public List<StatementApiDTO> getStatementsByKeyword(String keyword) {
        try {
            AssemblyApiResponse<String> response = searchStatementsByKeyword(keyword);
            if (!response.isSuccess()) {
                log.error("API 호출 실패: {}", response.resultMessage());
                return List.of();
            }

            return apiMapper.map(response);
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
                            .path("nauvppbxargkmyovh")
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

    public List<StatementApiDTO> searchStatements(String politician, String keyword) {
        if (politician != null) {
            List<StatementApiDTO> statements = getStatementsByPolitician(politician);

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
}

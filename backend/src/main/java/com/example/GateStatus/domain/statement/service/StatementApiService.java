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

    public List<StatementApiDTO> getStatementsByPolitician(String name) {
        AssemblyApiResponse<String> response = fetchStatementsByFigure(name);

        if (!response.isSuccess()) {
            log.error("API 호출 실패: {}", response.resultMessage());
            return List.of();
        }

        return apiMapper.map(response);
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
                        .path("/news/figure")
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
    private String extractResultCode(String xmlResponse) {
        if (xmlResponse == null) return "99";

        if (xmlResponse.contains("<CODE>")) {
            int start = xmlResponse.indexOf("<CODE>") + "<CODE>".length();
            int end = xmlResponse.indexOf("</CODE>");
            if (start > 0 && end > start) {
                return xmlResponse.substring(start, end);
            }
        }
        return "99";
    }

    private String extractResultMessage(String xmlResponse) {
        if (xmlResponse == null) return "응답이 없습니다";

        if (xmlResponse.contains("<MESSAGE>")) {
            int start = xmlResponse.indexOf("<MESSAGE>") + "<MESSAGE>".length();
            int end = xmlResponse.indexOf("</MESSAGE>");
            if (start > 0 && end > start) {
                return xmlResponse.substring(start, end);
            }
        }
        return "처리 중 오류가 발생했습니다";
    }
}

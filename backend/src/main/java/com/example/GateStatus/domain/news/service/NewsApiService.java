package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.NewsSource;
import com.example.GateStatus.domain.news.dto.NaverNewsResponse;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import com.example.GateStatus.global.config.exception.ApiClientException;
import com.example.GateStatus.global.config.exception.ApiServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.GateStatus.domain.common.HtmlUtils.removeHtmlTags;
import static com.example.GateStatus.domain.common.HtmlUtils.truncate;
import static com.example.GateStatus.domain.news.dto.NaverNewsResponse.Item.removeHtmlTags;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsApiService {

    private final WebClient webClient;
    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper;

    @Value("${news.api.naver.client-id}")
    private String naverClientId;

    @Value("${news.api.naver.client-secret}")
    private String naverClientSecret;

    @Value("${news.api.naver.base-url}")
    private String naverBaseUrl;

    @Value("${news.api.naver.rate-limit}")
    private int rateLimitPerDay;

    // 정치 관련 검색 키워드
    private static final List<String> POLITICAL_KEYWORDS = List.of(
            "국회", "법안", "정책", "대통령", "정치", "여당", "야당",
            "국정감사", "예산안", "개각", "선거", "정당"
    );

    // 네이버 뉴스 날짜 포맷
    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    /**
     *  네이버 뉴스 검색 API 호출
     * @param query
     * @param display
     * @param start
     * @param sort
     * @return
     */
    public List<NewsDocument> searchNaverNews(String query, int display, int start, String sort) {
        try {
            log.info("네이버 뉴스 검색 시작: query={}, display={}, start={}", query, display, start);

            NaverNewsResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/news.json")
                            .queryParam("query", query)
                            .queryParam("display", Math.min(display, 100))
                            .queryParam("start", start)
                            .queryParam("sort", sort)
                            .build())
                    .header("X-Naver-Client-Id", naverClientId)
                    .header("X-Naver-Client-Secret", naverClientSecret)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> Mono.error(new ApiClientException("네이버 API 클라이언트 오류")))
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> Mono.error(new ApiServerException("네이버 API 서버 오류")))
                    .bodyToMono(NaverNewsResponse.class)
                    .block();

            if (response == null || response.items() == null) {
                log.warn("네이버 뉴스 검색 결과 없음");
                return Collections.emptyList();
            }

            List<NewsDocument> newsDocuments = response.items().stream()
                    .map(item -> convertToNewsDocument(item, query))
                    .filter(this::isNotDuplicate)
                    .collect(Collectors.toList());

            log.info("네이버 뉴스 검색 완료: {}건 수집", newsDocuments.size());
            return newsDocuments;
        } catch (Exception e) {
            log.error("네이버 뉴스 검색 실패: query={}", query, e);
            throw new ApiClientException("뉴스 검색 중 오류 발생: " + e.getMessage());
        }
    }

    private NewsDocument convertToNewsDocument(NaverNewsResponse.Item item, String searchQuery) {
        String cleanTitle = removeHtmlTags(item.title());
        String cleanDescription = removeHtmlTags(item.description());

        cleanDescription = truncate(cleanDescription, 500);

        LocalDateTime pubDate = parsePubDate(item.pubDate());
        String contentHash = generateContentHash(cleanTitle, item.originallink());

        return NewsDocument.builder()
                .title(cleanTitle)
                .description(cleanDescription)
                .link(item.link())
                .originalLink(item.originallink())
                .pubDate(pubDate)
                .source(NewsSource.NAVER.name())
                .processed(false)
                .extractedKeywords(extractKeywords(cleanTitle + " " + cleanDescription))
                .category(categorizeNews(cleanTitle, cleanDescription))
                .contentHash(contentHash)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private boolean isNotDuplicate(Object object) {
        return false;
    }

    /**
     * 컨텐츠 해시 생성(중복 체크용)
     * @param title
     * @param url
     * @return
     */
    private String generateContentHash(String title, String url) {
        try {
            String content = title + url;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * 발행일 파싱
     */
    private LocalDateTime parsePubDate(String pubDateStr) {
        try {
            return LocalDateTime.parse(pubDateStr, NAVER_DATE_FORMAT);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 현재 시간으로 대체", pubDateStr);
            return LocalDateTime.now();
        }
    }
}

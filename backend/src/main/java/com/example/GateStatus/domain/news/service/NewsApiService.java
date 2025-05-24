package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

    public List<NewsDocument> searchNaverNews(String query, int display, int start, String sort) {

    }
}

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
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

@Service
@Slf4j
public class NewsApiService {

    private final WebClient naverWebClient;
    private final NewsRepository newsRepository;

    @Value("${news.api.naver.client-id}")
    private String naverClientId;

    @Value("${news.api.naver.client-secret}")
    private String naverClientSecret;

    @Value("${news.api.naver.rate-limit}")
    private int rateLimitPerDay;

    public NewsApiService(NewsRepository newsRepository, ObjectMapper objectMapper) {
        this.newsRepository = newsRepository;

        this.naverWebClient = WebClient.builder()
                .baseUrl("https://openapi.naver.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("NewsApiService WebClient 직접 생성 완료");
    }

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

            NaverNewsResponse response = naverWebClient.get()
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

            if (response == null || response.items() == null) { // record class
                log.warn("네이버 뉴스 검색 결과 없음");
                return Collections.emptyList();
            }

            List<NewsDocument> newsDocuments = response.items().stream()
                    .map(item -> convertToNewsDocument(item, query))
                    .filter(this::isNotDuplicate)
                    .collect(Collectors.toList());

            if (response.hasNextPage()) {
                log.debug("추가 검색 결과 존재: total={}, current={}",
                        response.total(), response.start() + response.display());
            }

            log.info("네이버 뉴스 검색 완료: {}건 수집", newsDocuments.size());
            return newsDocuments;
        } catch (Exception e) {
            log.error("네이버 뉴스 검색 실패: query={}", query, e);
            throw new ApiClientException("뉴스 검색 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 정치 카테고리 최신 뉴스 자동 수집
     * 매시간 실행되며 각 키워드별로 최신 뉴스 수집
     */
    @Scheduled(cron = "${news.api.schedule.collect-cron}")
    public void collectPoliticalNews() {
        log.info("정치 뉴스 자동 수집 시작");

        int totalCollected = 0;
        for (String keyword : POLITICAL_KEYWORDS) {
            try {
                Thread.sleep(100);

                List<NewsDocument> news = searchNaverNews(keyword, 20, 1, "date");
                List<NewsDocument> savedNews = newsRepository.saveAll(news);
                totalCollected += savedNews.size();

                log.debug("키워드 '{}' 뉴스 {}건 저장", keyword, savedNews.size());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("뉴스 수집 중단됨", e);
                break;
            } catch (Exception e) {
                log.error("키워드 '{}' 뉴스 수집 실패", keyword, e);
            }
        }

        log.info("정치 뉴스 자동 수집 완료: 총 {}건", totalCollected);
    }

    public List<NewsDocument> searchPoliticianNews(String politicianName, int days) {
        String query = String.format("%s AND (정치 OR 국회 OR 법안", politicianName);
        List<NewsDocument> allNews = new ArrayList<>();

        for (int start = 1; start <= 901; start += 100) {
            List<NewsDocument> news = searchNaverNews(query, 100, start, "date");

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            List<NewsDocument> filteredNews = news.stream()
                    .filter(n -> n.getPubDate().isAfter(cutoffDate))
                    .collect(Collectors.toList());

            allNews.addAll(filteredNews);

            // 최신 뉴스가 Cut off Date보다 이전이면 중단
            if (filteredNews.isEmpty() || news.size() < 100) {
                break;
            }
        }
        return allNews;
    }

    public Map<String, Integer> analyzeTrendingKeywords(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<NewsDocument> recentNews = newsRepository.findByPubDateAfter(since);

        Map<String, Integer> keywordFrequency = new HashMap<>();

        for (NewsDocument news : recentNews) {
            for (String keyword : news.getExtractedKeywords()) {
                keywordFrequency.merge(keyword, 1, Integer::sum);
            }
        }

        return keywordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 네이버 뉴스 아이템을 NewsDocument로 변환
     *
     * @param item
     * @param searchQuery
     * @return
     */
    private NewsDocument convertToNewsDocument(NaverNewsResponse.Item item, String searchQuery) {
        String cleanTitle = removeHtmlTags(item.title());
        String cleanDescription = removeHtmlTags(item.description());

        cleanDescription = truncate(cleanDescription, 500);

        LocalDateTime pubDate = parsePubDate(item.pubDate());
        String contentHash = generateContentHash(cleanTitle, item.originalLink());

        return NewsDocument.builder()
                .title(cleanTitle)
                .description(cleanDescription)
                .link(item.link())
                .originalLink(item.originalLink())
                .pubDate(pubDate)
                .source(NewsSource.NAVER.name())
                .processed(false)
                .extractedKeywords(extractKeywords(cleanTitle + " " + cleanDescription))
                .category(categorizeNews(cleanTitle, cleanDescription))
                .contentHash(contentHash)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 뉴스 카테고리 자동 분류 (한글기반)
     * 검색 시 사용하는 카테고리와 일치하도록 한글로 통일
     *
     */
    private String categorizeNews(String title, String description) {
        String content = (title + " " + description).toLowerCase();

        if (content.contains("대선") || content.contains("대통령선거") ||
            content.contains("대통령 후보") || content.contains("대통력직") ||
            content.contains("presidential") || content.contains("대통령 선거")) {
            return "대선";
        }

        if (content.contains("선거") || content.contains("공천") ||
            content.contains("후보") || content.contains("당선") ||
            content.contains("투표") || content.contains("개표")) {
            return "선거";
        }

        if (content.contains("국회") || content.contains("정상회담") ||
            content.contains("국정감사") || content.contains("법안") ||
            content.contains("입법") || content.contains("발언") ||
            content.contains("국정") || content.contains("의정")) {
            return "국회";
        }


    }

    /**
     * 간단한 키워드 추출
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        String lowerText = text.toLowerCase();

        for (String keyword : POLITICAL_KEYWORDS) {
            if (lowerText.contains(keyword.toLowerCase())) {
                keywords.add(keyword);
            }
        }

        return keywords.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }


    /**
     *  중복 뉴스 체크
     * @param news
     * @return
     */
    private boolean isNotDuplicate(NewsDocument news) {
        LocalDateTime since = LocalDateTime.now().minusWeeks(1);

        return newsRepository.findByContentHashAndCreatedAtAfter(news.getContentHash(), since).isEmpty();

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

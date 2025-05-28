package com.example.GateStatus.domain.news.controller;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.dto.*;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import com.example.GateStatus.domain.news.service.NewsApiService;
import com.example.GateStatus.domain.news.service.NewsService;
import com.example.GateStatus.domain.news.service.NewsStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;
    private final NewsApiService newsApiService;
    private final NewsRepository newsRepository;
    private final NewsStatisticsService newsStatisticsService;

    // 뉴스 조회
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getNews(@RequestParam(required = false) String category,
                                                      @PageableDefault(size = 10, sort = "pubDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NewsDocument> newsPage;

        if (category != null && !category.isEmpty()) {
            newsPage = newsService.getNewsByCategory(category, pageable);
        } else {
            newsPage = newsService.getRecentNews(pageable);
        }

        Page<NewsResponse> responses = newsPage.map(NewsResponse::from);
        return ResponseEntity.ok(responses);
    }

    /**
     * 디버깅용 컨트롤러 추가
     * 실제 저장된 뉴스 데이터 구조 확인
     */
    @GetMapping("/debug/news-data")
    public ResponseEntity<List<Map<String, Object>>> getNewsDataForDebug() {
        List<NewsDocument> recentNews = newsRepository.findTop10ByOrderByCreatedAtDesc();

        List<Map<String, Object>> debugData = recentNews.stream().map(news -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", news.getId());
            data.put("title", news.getTitle());
            data.put("category", news.getCategory());
            data.put("source", news.getSource());
            data.put("createdAt", news.getCreatedAt());
            return data;
        }).toList();

        return ResponseEntity.ok(debugData);
    }
    
    /**
     * 저장된 모든 카테고리 목록 확인
     */
    @GetMapping("/debug/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = newsRepository.findDistinctCategories();
        return ResponseEntity.ok(categories);
    }
    /**
     * 뉴스 단건 조회
     * @param newsId
     * @return
     */
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponse> getNewsDetail(@PathVariable String newsId) {
        NewsDocument news = newsService.getNews(newsId);
        return ResponseEntity.ok(NewsResponse.from(news));
    }

    // 수동 뉴스 수집 트리거 (테스트/관리자용)
    @PostMapping("/collect")
    public ResponseEntity<CollectResponse> triggerNewsCollection(@RequestParam(defaultValue = "정치") String keyword,
                                                      @RequestParam(defaultValue = "20") int count) {

        log.info("뉴스 수집 요청: keyword={}, count={}", keyword, count);

        try {
            List<NewsDocument> collected = newsApiService.searchNaverNews(keyword, count,1 , "date");
            List<NewsDocument> saved = collected.stream()
                    .map(newsService::saveNews)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new CollectResponse(
                    saved.size(),
                    "뉴스 수집 완료",
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("뉴스 수집 실패", e);
            return ResponseEntity.ok(new CollectResponse(
                    0,
                    "뉴스 수집 실패: " + e.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }

    /**
     * 정치 뉴스 자동 수집
     * @return
     */
    @PostMapping("/collect/political")
    public ResponseEntity<CollectResponse> collectPoliticalNews() {
        log.info("정치 뉴스 자동 수집 시작");

        try {
            newsApiService.collectPoliticalNews();

            return ResponseEntity.ok(new CollectResponse(
                    -1,
                    "정치 뉴스 수집 시작됨",
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("정치 뉴스 수집 실패", e);
            return ResponseEntity.status(500).body(new CollectResponse(
                    0,
                    "수집 실패: " + e.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }

    /**
     * 키워드 통계
     * @return
     */
    @GetMapping("/statistics/keywords")
    public ResponseEntity<Map<String, Long>> getKeywordStatistics() {
        Map<String, Long> keywords = newsStatisticsService.getKeywordFrequency();
        return ResponseEntity.ok(keywords);
    }

    // 트렌딩 키워드
    @GetMapping("/statistics/trending")
    public ResponseEntity<List<TrendingKeyword>> getTrendingKeywords(@RequestParam(defaultValue = "24") int hours) {
        List<TrendingKeyword> trending = newsStatisticsService.calculateTrendingKeywords(hours);
        return ResponseEntity.ok(trending);
    }

    /**
     * 카테고리별 통계
     * @param days
     * @return
     */
    @GetMapping("/statistics/categories")
    public ResponseEntity<Map<String, CategoryStats>> getCategoryStatistics(@RequestParam(defaultValue = "7") int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        LocalDateTime end = LocalDateTime.now();

        Map<String, CategoryStats> stats = newsStatisticsService.getCategoryDistribution(start, end);
        return ResponseEntity.ok(stats);
    }
}

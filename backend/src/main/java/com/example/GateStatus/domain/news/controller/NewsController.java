package com.example.GateStatus.domain.news.controller;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.dto.CollectResponse;
import com.example.GateStatus.domain.news.dto.NaverNewsResponse;
import com.example.GateStatus.domain.news.dto.NewsResponse;
import com.example.GateStatus.domain.news.dto.TrendingKeyword;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;
    private final NewsApiService newsApiService;
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
     * 뉴스 단건 조회
     * @param newsId
     * @return
     */
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponse> getNewsDetail(@PathVariable String newsId) {
        NewsDocument news = newsService.getNews(newsId);
        return ResponseEntity.ok(NewsResponse.from(news));
    }

    // 트렌딩 키워드
    @GetMapping("/trending")
    public List<TrendingKeyword> getTrendingKeywords() {
        return newsStatisticsService.calculateTrendingKeywords(24);
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
}

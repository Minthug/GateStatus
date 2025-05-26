package com.example.GateStatus.domain.news.controller;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.dto.TrendingKeyword;
import com.example.GateStatus.domain.news.service.NewsApiService;
import com.example.GateStatus.domain.news.service.NewsService;
import com.example.GateStatus.domain.news.service.NewsStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final NewsApiService newsApiService;
    private final NewsStatisticsService newsStatisticsService;

    // 뉴스 조회
    @GetMapping
    public Page<NewsDocument> getNews(@PageableDefault Pageable pageable) {
        return newsService.getRecentNews(pageable);
    }

    // 트렌딩 키워드
    @GetMapping("/trending")
    public List<TrendingKeyword> getTrendingKeywords() {
        return newsStatisticsService.calculateTrendingKeywords(24);
    }

    // 수동 뉴스 수집 트리거
    @PostMapping("/collect")
    public ResponseEntity<Void> triggerNewsCollection() {
        newsApiService.collectPoliticalNews();
        return ResponseEntity.ok().build();
    }
}

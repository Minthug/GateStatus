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
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public Page<NewsDocument> getNews(@PageableDefault Pageable pageable) {
        return newsService.getRecentNews(pageable);
    }

    @GetMapping("/trending")
    public List<TrendingKeyword> getTrendingKeywords() {

    }
}

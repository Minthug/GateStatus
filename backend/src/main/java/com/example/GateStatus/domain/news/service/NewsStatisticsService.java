package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsStatisticsService {

    private final NewsRepository newsRepository;

    /**
     * 가장 간단한 통계만 제공
     * @return
     */
    public Map<String, Long> getKeywordFrequency() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        List<NewsDocument> recentNews = newsRepository.findByPubDateAfter(weekAgo);

        return recentNews.stream()
                .flatMap(news -> news.getExtractedKeywords().stream())
                .collect(Collectors.groupingBy(
                        keyword -> keyword,
                        Collectors.counting()
                ));
    }
}

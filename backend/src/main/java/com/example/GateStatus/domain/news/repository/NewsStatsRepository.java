package com.example.GateStatus.domain.news.repository;

import com.example.GateStatus.domain.dashboard.dto.internal.KeywordCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NewsStatsRepository {

    private final MongoTemplate mongoTemplate;

    public List<KeywordCount> getTopKeywords(LocalDateTime startDate, LocalDateTime endDate, int limit) {

    }

    public List<NewsTime>
}

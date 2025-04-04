package com.example.GateStatus.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchTrendService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SEARCH_TREND_KEY = "search:trends:";
    private static final int MAX_TRENDS = 10;

    /**
     *
     * @param keyword
     */
    public void recordSearch(String keyword) {

        keyword = keyword.toLowerCase().trim();

        if (keyword.isEmpty()) {
            return;
        }

        redisTemplate.opsForZSet().incrementScore(SEARCH_TREND_KEY, keyword, 1);
    }

    public List<String> getTopSearches() {
        Set<Object> topSearches = redisTemplate.opsForZSet().reverseRange(SEARCH_TREND_KEY, 0, MAX_TRENDS - 1);

        if (topSearches == null) {
            return Collections.emptyList();
        }

        return topSearches.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * 0") // 매주 일요일 자정
    public void cleanupOldSearches() {
        Long size = redisTemplate.opsForZSet().size(SEARCH_TREND_KEY);

        if (size != null && size > 100) {
            redisTemplate.opsForZSet().removeRange(SEARCH_TREND_KEY, 0 , size - 101);
        }
    }
}

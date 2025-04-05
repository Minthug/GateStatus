package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.repository.response.IssueRedisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IssueRepository issueRepository;

    private static final String CACHE_KEY_PREFIX = "issue:";
    private static final String FIGURE_ISSUES_KEY_PREFIX = "figure:issues";
    private static final String HOT_ISSUES_KEY = "issues:hot";
    private static final long CACHE_TTL = 1800; // 30MIN

    /**
     * ID로 Issue 정보 조회 - 캐싱 적용
     * @param issueId
     * @return
     */
    public Issue findIssueById(Long issueId) {
        String cacheKey = CACHE_KEY_PREFIX + issueId;

        Issue cachedIssue = (Issue) redisTemplate.opsForValue().get(cacheKey);

        if (cachedIssue != null) {
            log.info("Cache hit for issue ID: {}", issueId);
        }

        return cachedIssue;
    }

    /**
     * 특정 Figure에 관련된 Issue 목록 조회
     * @param figureId
     * @return
     */
    public List<Issue> findIssuesByFigureId(Long figureId) {
        String cacheKey = FIGURE_ISSUES_KEY_PREFIX + figureId;

        List<Issue> cachedIssues = (List<Issue>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedIssues != null) {
            log.info("Cache hit for figure issues, figure Id: {}", figureId);
            return cachedIssues;
        }

        List<Issue> issues = issueRepository.findByFigureIdOrderByCreatedDateDesc(figureId);

        redisTemplate.opsForValue().set(cacheKey, issues, CACHE_TTL, TimeUnit.SECONDS);
        return issues;
    }

    public List<Issue> getHotIssues(int limit) {
        List<Issue> cachedHotIssues = (List<Issue>) redisTemplate.opsForValue().get(HOT_ISSUES_KEY);

        if (cachedHotIssues != null) {
            return cachedHotIssues;
        }

        List<Issue> hotIssues = issueRepository.findByIsHotOrderByViewCountDesc(true, PageRequest.of(0, limit));

        redisTemplate.opsForValue().set(HOT_ISSUES_KEY, hotIssues, 1, TimeUnit.HOURS);

        return hotIssues;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanOldIssues() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        redisTemplate.opsForZSet().removeRangeByScore(RECENT_ISSUES_KEY,
                0,
                weekAgo.toEpochSecond(ZoneOffset.UTC));
    }
}

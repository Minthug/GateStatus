package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    /**
     * 인기(HOT) 이슈 목록 조회
     * @param limit
     * @return
     */
    public List<Issue> getHotIssues(int limit) {
        List<Issue> cachedHotIssues = (List<Issue>) redisTemplate.opsForValue().get(HOT_ISSUES_KEY);

        if (cachedHotIssues != null) {
            return cachedHotIssues;
        }

        List<Issue> hotIssues = issueRepository.findByIsHotOrderByViewCountDesc(true, PageRequest.of(0, limit));

        redisTemplate.opsForValue().set(HOT_ISSUES_KEY, hotIssues, 1, TimeUnit.HOURS);

        return hotIssues;
    }

    /**
     * Issue 정보 업데이트 시 캐시 갱신
     * @param issue
     */
    public void updateIssueCache(Issue issue) {
        String cacheKey = CACHE_KEY_PREFIX + issue.getId();
        redisTemplate.opsForValue().set(cacheKey, issue, CACHE_TTL, TimeUnit.SECONDS);

        String figureIssuesKey = FIGURE_ISSUES_KEY_PREFIX + issue.getFigure().getId();
        redisTemplate.delete(figureIssuesKey);

        redisTemplate.delete(HOT_ISSUES_KEY);
        log.info("Updated cache for issue ID: {}", issue.getId());
    }

    /**
     * Issue 삭제 시 캐시 삭제
     * @param issue
     */
    public void evictIssueCache(Issue issue) {
        String cacheKey = CACHE_KEY_PREFIX + issue.getId();
        redisTemplate.delete(cacheKey);

        String figureIssuesKey = FIGURE_ISSUES_KEY_PREFIX + issue.getFigure().getId();
        redisTemplate.delete(figureIssuesKey);

        redisTemplate.delete(HOT_ISSUES_KEY);
        log.info("Evicted cache for issue ID: {}", issue.getId());
    }

    /**
     * 조회수 증가 시 캐시 업데이트
     * @param issueId
     */
    public void incrementViewCount(Long issueId) {
        String cacheKey = CACHE_KEY_PREFIX + issueId;
        Issue cachedIssue = (Issue) redisTemplate.opsForValue().get(cacheKey);

        if (cachedIssue != null) {
            cachedIssue.incrementViewCount();
            redisTemplate.opsForValue().set(cacheKey, cachedIssue, CACHE_TTL, TimeUnit.SECONDS);
        }
    }
}

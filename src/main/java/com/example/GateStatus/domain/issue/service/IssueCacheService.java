package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.repository.response.IssueRedisDto;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IssueRepository issueRepository;

    private static final String ISSUE_INFO_PREFIX = "issue:info";
    private static final String RECENT_ISSUES_KEY = "issues:recent";

    public void cacheIssueInfo(IssueRedisDto issueRedisDto){
        redisTemplate.opsForValue().set(
                ISSUE_INFO_PREFIX + issueRedisDto.issueId(),
                issueRedisDto,
                Duration.ofHours(24)
        );
    }

    public void addToRecentIssues(IssueRedisDto issueRedisDto) {
        redisTemplate.opsForZSet().add(
                RECENT_ISSUES_KEY,
                issueRedisDto,
                issueRedisDto.createdAt().toEpochSecond(ZoneOffset.UTC)
        );
        redisTemplate.opsForZSet().removeRange(RECENT_ISSUES_KEY, 0, -101);
    }

    public Optional<IssueRedisDto> getIssueInfo(Long issueId) {
        String key = ISSUE_INFO_PREFIX + issueId;
        IssueRedisDto cacheIssue = (IssueRedisDto) redisTemplate.opsForValue().get(key);

        if (cacheIssue != null) {
            return Optional.of(cacheIssue);
        }

        return issueRepository.findById(issueId)
                .map(issue -> {
                    IssueRedisDto issueRedisDto = IssueRedisDto.from(issue);
                    cacheIssueInfo(issueRedisDto);
                    return issueRedisDto;
                });
    }

    public List<IssueRedisDto> getRecentIssues(int limit) {
        Set<Object> issues = redisTemplate.opsForZSet()
                .reverseRange(RECENT_ISSUES_KEY, 0, limit - 1);

        if (issues != null || !issues.isEmpty()) {
            return issues.stream()
                    .filter(obj -> obj instanceof IssueRedisDto)
                    .map(obj -> (IssueRedisDto) obj)
                    .collect(Collectors.toList());
        }

        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        List<Issue> recentIssues = issueRepository.findAllByCreatedAtDesc(pageable);
        List<IssueRedisDto> issueRedisDtos = recentIssues.stream()
                .map(IssueRedisDto::from)
                .collect(Collectors.toList());

        issueRedisDtos.forEach(dto -> {
            cacheIssueInfo(dto);
            addToRecentIssues(dto);
        });
        return issueRedisDtos;
    }

    public void deleteIssueCache(Long issueId) {
        redisTemplate.delete(ISSUE_INFO_PREFIX + issueId);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanOldIssues() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        redisTemplate.opsForZSet().removeRangeByScore(RECENT_ISSUES_KEY,
                0,
                weekAgo.toEpochSecond(ZoneOffset.UTC));
    }
}

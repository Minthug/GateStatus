package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.repository.response.IssueRedisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;

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
}

package com.example.GateStatus.global.config.redis;

import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheIssue(IssueResponse issue) {
        redisTemplate.opsForValue().set("issue:" + issue.id(), issue, 1, TimeUnit.HOURS);
    }

    public Optional<IssueResponse> getCachedIssue(String issueId) {
        return Optional.ofNullable((IssueResponse) redisTemplate.opsForValue().get("issue:" + issueId));
    }
}

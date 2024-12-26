package com.example.GateStatus.domain.issue.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class issueStatsService {


    private static final String VIEW_COUNT_KEY = "issue:%d:views";
    private static final String TODAY_VIEW_KEY = "issue:%d:views:date:%s";
    private static final String IP_CHECK_KEY = "issue:%d:ip:%s:date:%s";
    private static final String HOT_ISSUE_KEY = "hot:issues";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void incrementViewCount(Long issueId, String ipAddress) {
        String today = LocalDate.now().toString();
        String ipKey = String.format(IP_CHECK_KEY, issueId, ipAddress, today);

        if (!redisTemplate.hasKey(ipKey)) {
            // IP 체크 설정
            redisTemplate.opsForValue().set(ipKey, "1", 24, TimeUnit.HOURS);

            String viewKey = String.format(VIEW_COUNT_KEY, issueId);
            redisTemplate.opsForValue().increment(viewKey);

            String todayKey = String.format(TODAY_VIEW_KEY, issueId, today);
            redisTemplate.opsForValue().increment(todayKey);

            redisTemplate.opsForZSet().incrementScore(HOT_ISSUE_KEY, issueId.toString(), 1);
        }
    }

    public List<Long> getHotIssues(int limit) {
        Set<String> hotIssues = redisTemplate.opsForZSet()
                .reverseRange(HOT_ISSUE_KEY, 0, limit - 1);

        return hotIssues.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void decreaseHotScores() {
        Set<String> issues = redisTemplate.opsForZSet().range(HOT_ISSUE_KEY, 0 , -1);
        for (String issueId : issues) {
            redisTemplate.opsForZSet().incrementScore(HOT_ISSUE_KEY, issueId, -0.5);
        }
    }
}

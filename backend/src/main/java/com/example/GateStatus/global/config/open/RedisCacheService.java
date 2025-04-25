package com.example.GateStatus.global.config.open;

import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 캐시에 값 저장
     * @param key
     * @param value
     * @param expirationSeconds
     */
    public void set(String key, Object value, long expirationSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, expirationSeconds, TimeUnit.SECONDS);
            log.debug("캐시 저장 성공: {}, 만료 시간: {}초", key, expirationSeconds);
        } catch (Exception e) {
            log.error("캐시 저장 실: {}", key, e);
        }
    }

    /**
     * 캐시에서 값 조회
     * @param key
     * @return
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("캐시 조회 실패: {}", key, e);
            return null;
        }
    }

    /**
     * 캐시에서 값 삭제
     * @param key
     * @return
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.error("캐시 삭제 실패: {}", e);
            return false;
        }
    }

    /**
     * 패턴에 맞는 모든 키 삭제
     * @param pattern
     * @return
     */
    public long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                return redisTemplate.delete(keys);
            }
            return 0;
        } catch (Exception e) {
            log.error("패턴 기반 캐시 삭제 실패: {}", pattern, e);
            return 0;
        }
    }

    /**
     * 캐시에 값이 있는지 확인
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("캐시 키 존재 확인 실패: {}", key, e);
            return false;
        }
    }

    /**
     * 캐시 만료 시간 설정/변경
     * @param key
     * @param expirationSeconds
     * @return
     */
    public boolean expire(String key, long expirationSeconds) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, expirationSeconds, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("캐시 만료 기간 설정 실패: {}", key, e);
            return false;
        }
    }

    /**
     * 캐시에 값이 없으면 주어진 supplier를 통해 값을 가져와 저장하고 변환
     * @param key
     * @param supplier
     * @param expirationSeconds
     * @return
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrSet(String key, Supplier<T> supplier, long expirationSeconds) {
        try {
            T cachedValue = (T) redisTemplate.opsForValue().get(key);

            if (cachedValue != null) {
                log.debug("캐시 히트: {}", key);
                return cachedValue;
            }

            log.debug("캐시 미스: {}", key);
            T newValue = supplier.get();

            if (newValue != null) {
                set(key, newValue, expirationSeconds);
            }

            return newValue;
        } catch (Exception e) {
            log.error("캐시 getOrSet 실패: {}", key, e);
            return supplier.get();
        }
    }

    /**
     * Issue 객체를 캐시에 저장
     * @param issue
     */
    public void cacheIssue(IssueResponse issue) {
        set("issue:" + issue.id(), issue, 3600); // 1hours
    }

    /**
     * 캐시된 Issue 객체 조회
     * @param issueId
     * @return
     */
    public Optional<IssueResponse> getCacheIssue(String issueId) {
        return Optional.ofNullable(get("issue:" + issueId));
    }
}

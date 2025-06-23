package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FigureCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    private final FigureRepository figureRepository;

    private static final String CACHE_KEY_PREFIX = "figure:";
    private static final String POPULAR_CACHE_KEY_PREFIX = "popular:figures:";
    private static final long CACHE_TTL = 3600; // 1 hour
    private static final long POPULAR_CACHE_TTL = 1800; // 30분


    public void cacheFigure(Figure figure) {
        if (figure == null || figure.getFigureId() == null) {
            log.warn("캐시할 Figure가 null이거나 figureId가 없습니다");
            return;
        }

        String cacheKey = CACHE_KEY_PREFIX + figure.getFigureId();
        try {
            redisTemplate.opsForValue().set(cacheKey, figure, CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Figure 캐시 저장 완료: {}", figure.getFigureId());
        } catch (Exception e) {
            log.error("Figure 캐시 저장 실패: {} - {}", figure.getFigureId(), e.getMessage());
        }
    }

    /**
     * ID로 Figure 정보 조회 - 캐싱 적용
     * @param figureId
     * @return
     */
    public Figure findFigureById(String figureId) {
        if (figureId == null || figureId.isEmpty()) {
            return null;
        }

        String cacheKey = CACHE_KEY_PREFIX + figureId;
        try {
            Figure cachedFigure = (Figure) redisTemplate.opsForValue().get(cacheKey);

            if (cachedFigure != null) {
                log.info("Cache hit for figure ID: {}", figureId);
            }
            return cachedFigure;
        } catch (Exception e) {
            log.info("Cache miss for figure ID: {}, loading from database", figureId);
            return null;
        }
    }

    /**
     * Figure 정보 업데이트 시 캐시 갱신
     * @param figure
     */
    public void updateFigureCache(Figure figure) {
        if (figure == null || figure.getFigureId() == null) {
            log.warn("업데이트할 Figure가 null이거나 figureId가 없습니다");
            return;
        }

        evictFigureCache(figure.getFigureId());
        cacheFigure(figure);
        log.debug("Figure 캐시 업데이트 완료: {}", figure.getFigureId());
    }

    /**
     * Figure 삭제 시 캐시 삭제
     * @param figureId
     */
    public void evictFigureCache(String figureId) {
        if (figureId == null || figureId.isEmpty()) {
            return;
        }

        String cacheKey = CACHE_KEY_PREFIX + figureId;
        try {
            redisTemplate.delete(cacheKey);

            Cache springCache = cacheManager.getCache("figures");
            if (springCache != null) {
                springCache.evict(figureId);
            }
            log.debug("Figure Cache delete Completed: {}", figureId);
        } catch (Exception e) {
            log.error("Figure 캐시 삭제 실패: {} - {}", figureId, e.getMessage());
        }
    }

    public void getPopularFigures(List<Figure> figures, int limit) {
        String cacheKey = "popular:figures:" + limit;

        try {
            redisTemplate.opsForValue().set(cacheKey, figures, POPULAR_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("인기 Figure 목록 캐시 저장 완료: limit={}", limit);
        } catch (Exception e) {
            log.error("인기 Figure 목록 캐시 저장 실패: limit={} - {}", limit, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Figure> getCachedPopularFigures(int limit) {
        String cacheKey = POPULAR_CACHE_KEY_PREFIX + limit;
        try {
            List<Figure> cachedList = (List<Figure>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedList != null) {
                log.debug("인기 Figure 목록 캐시 히트: limit={}", limit);
            }
            return cachedList;
        } catch (Exception e) {
            log.error("인기 Figure 목록 캐시 조회 실패: limit={} - {}", limit, e.getMessage());
            return null;
        }
    }

    public boolean isCached(String figureId) {
        if (figureId == null || figureId.isEmpty()) {
            return false;
        }

        try {
            Cache cache = cacheManager.getCache("figures");
            if (cache != null) {
                Cache.ValueWrapper value = cache.get(figureId);
                boolean cached = value != null;
                log.debug("Spring 캐시 상태({}): {}", figureId, cached ? "hit" : "miss");
                return cached;
            }
        } catch (Exception e) {
            log.error("캐시 상태 확인 실패: {} - {}", figureId, e.getMessage());
        }
        return false;
    }

    /**
     * 패턴에 맞는 모든 캐시 삭제
     * @param pattern 삭제할 캐시 키 패턴
     */
    public void evictCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("패턴 캐시 삭제 완료: {} ({}개)", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("패턴 캐시 삭제 실패: {} - {}", pattern, e.getMessage());
        }
    }

    /**
     * 모든 Figure 관련 캐시 삭제
     */
    public void clearAllFigureCache() {
        try {
            // Redis 수동 캐시 삭제
            evictCacheByPattern(CACHE_KEY_PREFIX + "*");
            evictCacheByPattern(POPULAR_CACHE_KEY_PREFIX + "*");

            // Spring Cache 삭제
            Cache figuresCache = cacheManager.getCache("figures");
            if (figuresCache != null) {
                figuresCache.clear();
            }

            Cache figureDtosCache = cacheManager.getCache("figure-dtos");
            if (figureDtosCache != null) {
                figureDtosCache.clear();
            }

            log.info("모든 Figure 캐시 삭제 완료");
        } catch (Exception e) {
            log.error("모든 Figure 캐시 삭제 실패: {}", e.getMessage());
        }
    }
}


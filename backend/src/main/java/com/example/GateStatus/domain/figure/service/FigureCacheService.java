package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FigureCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FigureRepository figureRepository;

    private static final String CACHE_KEY_PREFIX = "figure:";
    private static final long CACHE_TTL = 3600;

    /**
     * ID로 Figure 정보 조회 - 캐싱 적용
     * @param figureId
     * @return
     */
    public Figure findFigureById(Long figureId) {
        String cacheKey = CACHE_KEY_PREFIX + figureId;

        Figure cachedFigure = (Figure) redisTemplate.opsForValue().get(cacheKey);

        if (cachedFigure != null) {
            log.info("Cache hit for figure ID: {}", figureId);
            return cachedFigure;
        }

        log.info("Cache miss for figure ID: {}, loading from database", figureId);
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));

        redisTemplate.opsForValue().set(cacheKey, figure, CACHE_TTL, TimeUnit.SECONDS);

        return figure;
    }

    /**
     * Figure 정보 업데이트 시 캐시 갱신
     * @param figure
     */
    public void updateFigureCache(Figure figure) {
        String cacheKey = CACHE_KEY_PREFIX + figure.getId();
        redisTemplate.opsForValue().set(cacheKey, figure, CACHE_TTL, TimeUnit.SECONDS);
        log.info("Update cache for Figure ID: {}", figure.getId());
    }

    /**
     * Figure 삭제 시 캐시 삭제
     * @param figureId
     */
    public void evictFigureCache(Long figureId) {
        String cacheKey = CACHE_KEY_PREFIX + figureId;
        redisTemplate.delete(cacheKey);
        log.info("Evicted cache for Figure ID: {}", figureId);
    }

    public List<Figure> getPopularFigures(int limit) {
        String cacheKey = "popular:figures:" + limit;
        List<Figure> cachedList = (List<Figure>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedList != null) {
            return cachedList;
        }

        List<Figure> popularFigures = figureRepository.findTopByOrderByViewCountDesc(PageRequest.of(0, limit));
        redisTemplate.opsForValue().set(cacheKey, popularFigures, 1, TimeUnit.HOURS);

        return popularFigures;
    }


    /**
     * 조회수 증가 시 캐시 업데이트
     * @param figureId
     */
//    public void incrementViewCount(Long figureId) {
//        String cacheKey = CACHE_KEY_PREFIX + figureId;
//        Issue cachedIssue = (Issue) redisTemplate.opsForValue().get(cacheKey);
//
//        if (cachedIssue != null) {
//            cachedIssue.incrementViewCount();
//            redisTemplate.opsForValue().set(cacheKey, cachedIssue, CACHE_TTL, TimeUnit.SECONDS);
//        }
//    }
}

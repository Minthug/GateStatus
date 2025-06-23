package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class FigureCacheService {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final FigureRepository figureRepository;
//
//    private static final String CACHE_KEY_PREFIX = "figure:";
//    private static final long CACHE_TTL = 3600;
//
//    /**
//     * ID로 Figure 정보 조회 - 캐싱 적용
//     * @param figureId
//     * @return
//     */
//    @Cacheable(value = "figures", key = "#figureId")
//    public Figure findFigureById(String figureId) {
//        String cacheKey = CACHE_KEY_PREFIX + figureId;
//
//        Figure cachedFigure = (Figure) redisTemplate.opsForValue().get(cacheKey);
//
//        if (cachedFigure != null) {
//            log.info("Cache hit for figure ID: {}", figureId);
//            return cachedFigure;
//        }
//
//        log.info("Cache miss for figure ID: {}, loading from database", figureId);
//        Figure figure = figureRepository.findByFigureId(figureId)
//                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));
//
//        redisTemplate.opsForValue().set(cacheKey, figure, CACHE_TTL, TimeUnit.SECONDS);
//
//        return figure;
//    }
//
//    /**
//     * Figure 정보 업데이트 시 캐시 갱신
//     * @param figure
//     */
//    @CacheEvict(value = "figures", key = "#figure.figureId")
//    public void updateFigureCache(Figure figure) {
//        String cacheKey = CACHE_KEY_PREFIX + figure.getId();
//        redisTemplate.opsForValue().set(cacheKey, figure, CACHE_TTL, TimeUnit.SECONDS);
//        log.info("Update cache for Figure ID: {}", figure.getId());
//    }
//
//    /**
//     * Figure 삭제 시 캐시 삭제
//     * @param figureId
//     */
//    public void evictFigureCache(String figureId) {
//        String cacheKey = CACHE_KEY_PREFIX + figureId;
//        redisTemplate.delete(cacheKey);
//        log.info("Evicted cache for Figure ID: {}", figureId);
//    }
//
//    public List<Figure> getPopularFigures(int limit) {
//        String cacheKey = "popular:figures:" + limit;
//        List<Figure> cachedList = (List<Figure>) redisTemplate.opsForValue().get(cacheKey);
//
//        if (cachedList != null) {
//            return cachedList;
//        }
//
//        List<Figure> popularFigures = figureRepository.findTopByOrderByViewCountDesc(PageRequest.of(0, limit));
//        redisTemplate.opsForValue().set(cacheKey, popularFigures, 1, TimeUnit.HOURS);
//
//        return popularFigures;
//    }
//
//
//    @Cacheable(value = "figure-dtos", key = "#figureId", unless = "#result == null")
//    @Transactional(readOnly = true)
//    public FigureDTO findFigureDtoById(String figureId) {
//
//        log.info("항상 실행되는 로그: {}", figureId); // 항상 출력되는 로그 추가
//
//        try {
//            Figure figure = figureRepository.findByFigureId(figureId)
//                .orElseThrow(() -> new EntityNotFoundException("해당 국회의원을 찾을 수 없습니다: " + figureId));
//            log.info("국회의원 정보 조회 성공: {}, ID={}", figure.getName(), figureId);
//            return FigureDTO.from(figure);
//        } catch (Exception e) {
//            log.error("국회의원 정보 조회 실패: {} - {}", figureId, e.getMessage());
//            throw e;
//        }
//    }
//}


package com.example.GateStatus.domain.figure.service.core;

import com.example.GateStatus.domain.common.JsonUtils;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class FigureCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final FigureRepository figureRepository;

    private static final String CACHE_KEY_PREFIX = "figure:";
    private static final String POPULAR_CACHE_KEY_PREFIX = "popular:figures:";
    private static final String PARTY_CACHE_KEY_PREFIX = "party:figures:";
    private static final String SEARCH_CACHE_KEY_PREFIX = "search:figures:";

    private static final long CACHE_TTL = 3600; // 1 hour
    private static final long POPULAR_CACHE_TTL = 1800; // 30분
    private static final long PARTY_CACHE_TTL = 3600; // 1시간
    private static final long SEARCH_CACHE_TTL = 600; // 10분

    public void cacheFigure(FigureDTO dto) {
        if (dto == null || JsonUtils.isEmpty(dto.getFigureId())) {
            log.warn("캐시할 FigureDTO가 null이거나 figureId가 없습니다");
            return;
        }

        String cacheKey = CACHE_KEY_PREFIX + dto.getFigureId();
        try {
            String jsonValue = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(cacheKey, dto, CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Figure 캐시 저장 완료: {}", dto.getFigureId());
        } catch (Exception e) {
            log.error("Figure 캐시 저장 실패: {} - {}", dto.getFigureId(), e.getMessage());
        }
    }

    /**
     * ID로 Figure 정보 조회 - 캐싱 적용
     * @param figureId
     * @return
     */
    public FigureDTO getCachedFigure(String figureId) {
        if (JsonUtils.isEmpty(figureId)) {
            return null;
        }

        String cacheKey = CACHE_KEY_PREFIX + figureId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached instanceof String) {
                FigureDTO result = objectMapper.readValue((String) cached, FigureDTO.class);
                log.debug("FigureDTO 캐시 히트: {}", figureId);
                return result;
            }

            log.debug("FigureDTO 캐시 미스: {}", figureId);
            return null;
        } catch (Exception e) {
            log.warn("FigureDTO 캐시 조회 실패: {} - {}", figureId, e.getMessage());
            return null;
        }
    }

    // ========== getOrCompute 패턴 ==========
    /**
     * FigureDTO 전용 getOrCompute
     * @param figureId 국회의원 ID
     * @param supplier DB에서 조회하는 공급자
     * @return FigureDTO
     */
    public FigureDTO getOrComputeFigure(String figureId, Supplier<FigureDTO> supplier) {
        // 1. 캐시에서 먼저 조회
        FigureDTO cached = getCachedFigure(figureId);
        if (cached != null) {
            return cached;
        }

        // 2. 캐시 미스 - DB에서 조회
        log.debug("캐시 미스 - DB에서 조회: {}", figureId);
        FigureDTO result = supplier.get();

        // 3. 결과 캐싱 (null이 아닌 경우만)
        if (result != null) {
            cacheFigure(result);
        }

        return result;
    }

    /**
     * 리스트 전용 getOrCompute
     * @param cacheKey 캐시 키
     * @param ttl TTL (초)
     * @param supplier 리스트를 생성하는 공급자
     * @return FigureDTO 리스트
     */
    public List<FigureDTO> getOrComputeList(String cacheKey, long ttl, Supplier<List<FigureDTO>> supplier) {
        try {
            // 1. 캐시에서 조회
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached instanceof String) {
                JavaType listType = objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, FigureDTO.class);
                List<FigureDTO> result = objectMapper.readValue((String) cached, listType);
                log.debug("리스트 캐시 히트: {} ({}개)", cacheKey, result.size());
                return result;
            }

            // 2. 캐시 미스 - 새로 계산
            log.debug("리스트 캐시 미스 - 새로 계산: {}", cacheKey);
            List<FigureDTO> result = supplier.get();

            // 3. 결과 캐싱 (빈 리스트가 아닌 경우만)
            if (result != null && !result.isEmpty()) {
                String jsonValue = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, jsonValue, ttl, TimeUnit.SECONDS);
                log.debug("리스트 캐싱 완료: {} ({}개)", cacheKey, result.size());
            }

            return result != null ? result : Collections.emptyList();

        } catch (Exception e) {
            log.error("리스트 getOrCompute 실패: {} - {}", cacheKey, e.getMessage());
            return supplier.get();
        }
    }

    // ========== 특화된 캐시 메서드들 ==========

    /**
     * 인기 국회의원 조회 (캐시 적용)
     * @param limit 조회 개수
     * @param supplier DB 조회 로직
     * @return 인기 국회의원 목록
     */
    public List<FigureDTO> getOrComputePopularFigures(int limit, Supplier<List<FigureDTO>> supplier) {
        String cacheKey = POPULAR_CACHE_KEY_PREFIX + limit;
        return getOrComputeList(cacheKey, POPULAR_CACHE_TTL, supplier);
    }

    /**
     * 정당별 국회의원 조회 (캐시 적용)
     * @param party 정당
     * @param supplier DB 조회 로직
     * @return 정당 소속 국회의원 목록
     */
    public List<FigureDTO> getOrComputePartyFigures(FigureParty party, Supplier<List<FigureDTO>> supplier) {
        String cacheKey = PARTY_CACHE_KEY_PREFIX + party.name();
        return getOrComputeList(cacheKey, PARTY_CACHE_TTL, supplier);
    }

    /**
     * 검색 결과 캐시 (키워드별)
     * @param keyword 검색 키워드
     * @param supplier DB 검색 로직
     * @return 검색 결과 목록
     */
    public List<FigureDTO> getOrComputeSearchResults(String keyword, Supplier<List<FigureDTO>> supplier) {
        // 키워드 정규화 (공백 제거, 소문자 변환)
        String normalizedKeyword = keyword.trim().toLowerCase();
        String cacheKey = SEARCH_CACHE_KEY_PREFIX + normalizedKeyword;

        return getOrComputeList(cacheKey, SEARCH_CACHE_TTL, supplier);
    }

    // ========== 캐시 무효화 메서드들 ==========

    /**
     * 특정 국회의원 캐시 삭제
     * @param figureId 국회의원 ID
     */
    public void evictFigureCache(String figureId) {
        if (JsonUtils.isEmpty(figureId)) {
            return;
        }

        try {
            // Redis 캐시 삭제
            String cacheKey = CACHE_KEY_PREFIX + figureId;
            redisTemplate.delete(cacheKey);

            // Spring Cache 삭제
            Cache springCache = cacheManager.getCache("figures");
            if (springCache != null) {
                springCache.evict(figureId);
            }

            Cache dtoCache = cacheManager.getCache("figure-dtos");
            if (dtoCache != null) {
                dtoCache.evict(figureId);
            }

            log.debug("Figure 캐시 삭제 완료: {}", figureId);
        } catch (Exception e) {
            log.error("Figure 캐시 삭제 실패: {} - {}", figureId, e.getMessage());
        }
    }

    /**
     * 관련된 리스트 캐시들 무효화
     * - 국회의원 정보가 변경되면 인기순, 정당별, 검색 결과도 무효화
     */
    public void evictRelatedCaches() {
        try {
            evictCacheByPattern(POPULAR_CACHE_KEY_PREFIX + "*");
            evictCacheByPattern(PARTY_CACHE_KEY_PREFIX + "*");
            evictCacheByPattern(SEARCH_CACHE_KEY_PREFIX + "*");
            log.debug("관련 캐시들 무효화 완료");
        } catch (Exception e) {
            log.error("관련 캐시 무효화 실패: {}", e.getMessage());
        }
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
                log.debug("패턴 캐시 삭제 완료: {} ({}개)", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("패턴 캐시 삭제 실패: {} - {}", pattern, e.getMessage());
        }
    }

    /**
     * 모든 Figure 관련 캐시 삭제
     */
    public void evictAllCache() {
        try {
            // Redis 캐시 삭제
            evictCacheByPattern(CACHE_KEY_PREFIX + "*");
            evictCacheByPattern(POPULAR_CACHE_KEY_PREFIX + "*");
            evictCacheByPattern(PARTY_CACHE_KEY_PREFIX + "*");
            evictCacheByPattern(SEARCH_CACHE_KEY_PREFIX + "*");

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


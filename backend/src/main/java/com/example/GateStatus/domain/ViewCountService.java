package com.example.GateStatus.domain;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final RedisTemplate<String, Long> stringLongRedisTemplate;
    private final FigureRepository figureRepository;

    private static final String VIEW_COUNT_PREFIX = "view:figure:";

    /**
     * 정치인 프로필 조회수 증가
     * @param figureId
     */
    public void incrementViewCount(Long figureId) {
        String key = VIEW_COUNT_PREFIX + figureId;
        stringLongRedisTemplate.opsForValue().increment(key, 1L);
    }

    /**
     *
     * @param figureId
     * @return
     */
    public Long getViewCount(Long figureId) {
        String key = VIEW_COUNT_PREFIX + figureId;
        Long count = stringLongRedisTemplate.opsForValue().get(key);
        return count != null ? count : 0L;
    }

}

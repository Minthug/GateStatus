//package com.example.GateStatus.domain;
//
//import com.example.GateStatus.domain.figure.repository.FigureRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//
//@Service
//@RequiredArgsConstructor
//public class ViewCountService {
//
//    private final RedisTemplate<String, Long> stringLongRedisTemplate;
//
//    private static final String VIEW_COUNT_PREFIX = "view:figure:";
//
//    /**
//     * 정치인 프로필 조회수 증가
//     * @param figureId
//     */
//    public void incrementViewCount(Long figureId) {
//        String key = VIEW_COUNT_PREFIX + figureId;
//        stringLongRedisTemplate.opsForValue().increment(key, 1L);
//    }
//
//    /**
//     *
//     * @param figureId
//     * @return
//     */
//    public Long getViewCount(Long figureId) {
//        String key = VIEW_COUNT_PREFIX + figureId;
//        Long count = stringLongRedisTemplate.opsForValue().get(key);
//        return count != null ? count : 0L;
//    }
//
//    @Scheduled(fixedRate = 1800000)
//    public void syncViewCountsToDB() {
//        Set<String> keys = stringLongRedisTemplate.keys(VIEW_COUNT_PREFIX + "*");
//
//        if (keys == null || keys.isEmpty()) {
//            return;
//        }
//
//        for (String key : keys) {
//            try {
//                String figureIdStr = key.substring(VIEW_COUNT_PREFIX.length());
//                Long figureId = Long.parseLong(figureIdStr);
//                Long viewCount = stringLongRedisTemplate.opsForValue().get(key);
//
//                if (viewCount != null) {
//                    figureRepository.updateViewCount(figureId, viewCount);
//                }
//            } catch (Exception e) {
//
//            }
//        }
//    }
//}

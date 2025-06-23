package com.example.GateStatus.domain.figure.service.core;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureService {

    private final FigureQueryService queryService;
    private final FigureCacheService cacheService;

    @Transactional(readOnly = true)
    public FigureDTO getFigure(String figureId) {
        log.debug("국회의원 조회 요청: {}", figureId);

        // 1. 캐시에서 먼저 조회
        FigureDTO cached = cacheService.getCachedFigure(figureId);
        if (cached != null) {
            log.debug("캐시에서 조회 성공: {}", figureId);
            return cached;
        }

        // 2. DB에서 조회 후 캐싱
        Figure figure = queryService.findByFigureId(figureId);
        FigureDTO dto = FigureDTO.from(figure);

        // 3. 조회수 증가 (비동기)
        queryService.incrementViewCountAsync(figureId);

        // 4. 캐시 저장
        cacheService.cacheFigure(dto);

        log.debug("DB에서 조회 후 캐싱 완료: {}", figureId);
        return dto;
    }


}

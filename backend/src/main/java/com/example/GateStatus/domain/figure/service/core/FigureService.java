package com.example.GateStatus.domain.figure.service.core;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.request.FigureSearchRequest;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureService {

    private final FigureQueryService queryService;
    private final FigureCacheService cacheService;
    private final FigureRepository figureRepository;

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

    @Transactional(readOnly = true)
    public Page<FigureDTO> getFigures(FigureSearchRequest request) {
        log.debug("국회의원 목록 조회: {}", request);

        Page<Figure> figures = queryService.findAllWithCriteria(request);
        return figures.map(FigureDTO::from);
    }

    @Transactional(readOnly = true)
    public List<FigureDTO> getPopularFigures(int limit) {
        log.debug("인기 국회의원 조회: {}명", limit);

        String cacheKey = "popular_figures_" + limit;

        return cacheService.getOrCompute(cacheKey, 1800, () -> {
            List<Figure> figures = queryService.findPopularFigures(limit);
            return figures.stream()
                    .map(FigureDTO::from)
                    .collect(Collectors.toList());
        });
    }

    @Transactional(readOnly = true)
    public List<FigureDTO> getFiguresByParty(FigureParty party) {
        log.debug("정당별 국회의원 조회: {}", party);

        String cacheKey = "party_figures_" + party.name();

        return cacheService.getOrCompute(cacheKey, 3600, () -> {
            List<Figure> figures = queryService.findByParty(party);
            return figures.stream()
                    .map(FigureDTO::from)
                    .collect(Collectors.toList());
        });
    }

    @Transactional(readOnly = true)
    public List<FigureDTO> getFiguresByType(FigureType type) {
        if (type == null) {
            return Collections.emptyList();
        }

        String cacheKey = "type_figures_" + type.getDisplayName();

        return cacheService.getOrCompute(cacheKey, 3600, () -> {
            List<Figure> figures = queryService.findByType(type);
            return figures.stream()
                    .map(FigureDTO::from)
                    .collect(Collectors.toList());
        });
    }

    @Transactional(readOnly = true)
    public List<FigureDTO> searchFigures(String keyword) {
        return cacheService.getOrComputeSearchResults(keyword, () -> {
            List<Figure> figures = figureRepository.findByNameContainingOrConstituencyContaining(keyword, keyword);
            return figures.stream()
                    .map(FigureDTO::from)
                    .collect(Collectors.toList());
        });
    }

    @Transactional(readOnly = true)
    public boolean existsFigure(String figureId) {
        return queryService.existsByFigureId(figureId);
    }

}

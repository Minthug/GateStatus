package com.example.GateStatus.domain.figure.service.core;

import com.example.GateStatus.domain.common.JsonUtils;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.request.FigureSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FigureQueryService {

    private final FigureRepository figureRepository;

    public Figure findByFigureId(String figureId) {
        return figureRepository.findByFigureId(figureId)
                .orElseThrow(() -> new NotFoundFigureException("국회의원을 찾을 수 없습니다 " + figureId));
    }

    public Optional<Figure> findByName(String name) {
        return figureRepository.findByName(name);
    }

    @Async
    public void incrementViewCountAsync(String figureId) {
        try {
            figureRepository.incrementViewCount(figureId);
            log.debug("조회수 증가: {}", figureId);
        } catch (Exception e) {
            log.warn("조회수 증가 실패: {} - {}", figureId, e.getMessage());
        }
    }

    public Page<Figure> findAllWithCriteria(FigureSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy())
        );

        if (JsonUtils.isNotEmpty(request.getKeyword())) {
            return findBySearchType(request, pageable);
        } else if (request.getParty() != null) {
            return figureRepository.findByFigureParty(request.getParty(), pageable);
        } else {
            return figureRepository.findAll(pageable);
        }
    }

    public List<Figure> findByParty(FigureParty party) {
        return figureRepository.findAllByFigureParty(party);
    }

    public List<Figure> findPopularFigures(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return figureRepository.findTopByOrderByViewCountDesc(pageable);
    }

    public List<Figure> findRecentlyUpdated(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return figureRepository.findTop10ByOrderByModifiedDateDesc(pageable);
    }

    public boolean existsByFigureId(String figureId) {
        return figureRepository.existsByFigureId(figureId);
    }

    public long getTotalCount() {
        return figureRepository.count();
    }

    // ========== Private 메서드들 ==========

    private Page<Figure> findBySearchType(FigureSearchRequest request, Pageable pageable) {
        return switch (request.getSearchType()) {
            case "name" -> figureRepository.findByNameContaining(request.getKeyword(), pageable);
            case "constituency" -> figureRepository.findByConstituencyContaining(request.getKeyword(), pageable);
            case "party" -> findByPartyName(request.getKeyword(), pageable);
            default -> figureRepository.findByNameContainingOrConstituencyContaining(
                    request.getKeyword(), request.getKeyword(), pageable);
        };
    }

    private Page<Figure> findByPartyName(String partyKeyword, Pageable pageable) {
        // 정당명으로 FigureParty enum 찾기
        FigureParty party = Arrays.stream(FigureParty.values())
                .filter(p -> p.getPartyName().contains(partyKeyword))
                .findFirst()
                .orElse(null);

        return party != null ?
                figureRepository.findByFigureParty(party, pageable) :
                Page.empty(pageable);
    }

    public List<Figure> findByType(FigureType figureType) {
        if (figureType == null) {
            return Collections.emptyList();
        }

        try {
            List<Figure> figures = figureRepository.findByFigureType(figureType);

            return figures;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}


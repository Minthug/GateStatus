package com.example.GateStatus.domain.figure.service.core;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.external.FigureApiService;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.statement.service.StatementSyncService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureService {

    private final FigureRepository figureRepository;
    private final FigureApiService figureApiService;
    private final FigureCacheService figureCacheService;
    private final CacheManager cacheManager;
    private final StatementSyncService syncService;

    /**
     * 국회의원 등록 또는 조회
     * @param command
     * @return
     */
//    @Transactional
//    public RegisterFigureResponse getRegisterFigure(final RegisterFigureCommand command) {
//        Figure findFigure = figureRepository.findByName(command.name())
//                .orElseGet(() -> {
//                    log.info("신규 국회의원 등록: {}", command.name());
//                    Figure figure = Figure.builder()
//                            .name(command.name())
//                            .englishName(command.englishName())
//                            .birth(command.birth())
//                            .constituency(command.constituency())
//                            .profileUrl(command.profileUrl())
//                            .figureType(command.figureType())
//                            .education(command.education())
//                            .careers(command.careers())
//                            .sites(command.sites())
//                            .activities(command.activities())
//                            .updateSource(command.updateSource())
//                            .build();
//                    Figure savedFigure = figureRepository.save(figure);
//                    if (savedFigure.getFigureId() != null) {
//                        figureCacheService.updateFigureCache(savedFigure);
//                    }
//
//                    return savedFigure;
//                });
//        return RegisterFigureResponse.from(findFigure);
//    }

    /**
     * 국회의원 상세 정보 조회
     * @param command
     * @return
     */
//    @Transactional(readOnly = true)
//    public FindFigureDetailResponse findFigure(FindFigureCommand command) {
//        if (command.figureId() == null || command.figureId().isEmpty()) {
//            throw new IllegalArgumentException("국회의원 ID는 필수 값입니다");
//        }
//
//        log.debug("조회 시도 figureId: {}", command.figureId());
//
//        try {
//            Figure findFigure = figureCacheService.findFigureById(command.figureId());
//            return FindFigureDetailResponse.from(findFigure);
//        } catch (EntityNotFoundException e) {
//            log.warn("국회의원 정보 조회 실패: {}", command.figureId());
//            throw e;
//        }
//    }

    /**
     * 캐시 상태 확인 (모니터링 용도)
     * @param figureId
     * @return
     */
    public boolean checkCacheStatus(String figureId) {
        if (figureId == null || figureId.isEmpty()) {
            return false;
        }

        Cache cache = cacheManager.getCache("figures");
        Cache.ValueWrapper springCacheValue = cache != null ? cache.get(figureId) : null;

        boolean isCached = springCacheValue != null;
        log.debug("캐시 상태({}): {}", figureId, isCached ? "hit" : "miss");

        return isCached;
    }

    /**
     * 검색 조건에 따른 국회의원 목록 조회
     * @param pageRequest
     * @param type
     * @param keyword
     * @return
     */
    @Transactional(readOnly = true)
    public Page<FindFigureDetailResponse> findAllFigures(PageRequest pageRequest, String type, String keyword) {
        Page<Figure> figures;

        if (type != null && keyword != null && !keyword.isEmpty()) {
            log.debug("국회의원 검색: type={}, keyword={}", type, keyword);
            figures = switch (type.toLowerCase()) {
                case "name" -> figureRepository.findByNameContaining(keyword, pageRequest);
                case "place" -> figureRepository.findByConstituencyContaining(keyword, pageRequest);
                default -> figureRepository.findAll(pageRequest);
            };
        } else {
          figures = figureRepository.findAll(pageRequest);
        }
        return figures.map(FindFigureDetailResponse::from);
    }

    /**
     * 국회의원 ID로 정보 조회
     * @param figureId
     * @return
     */
//    @Transactional(readOnly = true)
//    public FigureDTO findFigureById(String figureId) {
//        if (figureId == null || figureId.isEmpty()) {
//            throw new IllegalArgumentException("국회의원 ID는 필수 값입니다");
//        }
//
//        try {
//            return figureCacheService.findFigureDtoById(figureId);
//        } catch (EntityNotFoundException e) {
//            log.warn("국회의원 정보 조회 실패: {}", figureId);
//            throw e;
//        }
//    }

    /**
     * 타입별 국회의원 목록 조회
     * @param figureType
     * @return
     */
    @Transactional(readOnly = true)
    public List<FindFigureDetailResponse> findFiguresByType(FigureType figureType) {

        if (figureType == null) {
            throw new IllegalArgumentException("국회의원 타입은 필수 값 입니다");
        }

        log.debug("타입별 국회의원 조회: {}", figureType);
        return figureRepository.findByFigureType(figureType)
                .stream()
                .map(FindFigureDetailResponse::from)
                .collect(Collectors.toList());
    }

//    @Transactional
//    public UpdateFigureResponse updateFigure(String figureId, UpdateFigureCommand command) {
//        if (figureId == null || figureId.isEmpty()) {
//            throw new IllegalArgumentException("국회의원 ID는 필수 값 입니다");
//        }
//
//        log.info("국회의원 정보 업데이트: {}", figureId);
//        Figure figure = figureRepository.findByFigureId(figureId)
//                .orElseThrow(() -> new NotFoundFigureException("국회의원을 찾을 수 없습니다" + figureId));
//
//        figure.update(
//                command.name(),
//                command.englishName(),
//                command.birth(),
//                command.constituency(), // place 대신 constituency 사용
//                command.profileUrl(),
//                command.figureType(),
//                command.figureParty(),
//                command.education(),
//                command.careers(),
//                command.sites(),
//                command.activities(),
//                command.updateSource()
//        );
//
//        Figure updateFigure = figureRepository.save(figure);
//
//        figureCacheService.updateFigureCache(updateFigure);
//        log.debug("국회의원 정보 업데이트 완료: {}", updateFigure.getName());
//
//        return UpdateFigureResponse.from(updateFigure);
//    }


//    @Transactional
//    public void deleteFigure(String figureId) {
//        if (figureId == null || figureId.isEmpty()) {
//            throw new IllegalArgumentException("국회의원 ID는 필수 값입니다");
//        }
//
//        log.info("국회의원 정보 삭제: {}", figureId);
//        Figure figure = figureRepository.findByFigureId(figureId)
//                .orElseThrow(() -> new EntityNotFoundException("해당 국회의원을 찾을 수 없습니다: " + figureId));
//
//        figureRepository.delete(figure);
//        figureCacheService.evictFigureCache(figureId);
//    }
//
//    @Transactional(readOnly = true)
//    public List<FindFigureDetailResponse> getPopularFigures(int limit) {
//        log.debug("인기 국회의원 조회: limit={}", limit);
//        return figureCacheService.getPopularFigures(limit)
//                .stream()
//                .map(FindFigureDetailResponse::from)
//                .collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public FigureDTO findFigureByName(String name) {
        log.info("이름으로 국회의원 검색: {}", name);

        // 1. DB에서 먼저 검색
        Figure figure = figureRepository.findByName(name)
                .orElse(null);

        if (figure != null) {
            log.debug("DB에서 국회의원 정보 찾음: {}", name);
            return FigureDTO.from(figure);
        }

        // 2. DB에 없으면 API에서 동기화 시도
        try {
            log.info("DB에 없어 API에서 국회의원 정보 동기화 시도: {}", name);
            figure = figureApiService.syncFigureInfoByName(name);

            if (figure != null) {
                return FigureDTO.from(figure);
            }
        } catch (Exception e) {
            log.warn("국회의원 정보 동기화 실패: {} - {}", name, e.getMessage());
        }

        return null;
    }


    @Transactional
    public Figure ensureFigureExists(String figureName, boolean forceSync, boolean syncStatements) {
        log.info("정치인 존재 확인: {}, 강제동기화: {}", figureName, forceSync);

        Figure figure = figureRepository.findByName(figureName).orElse(null);

        if (figure == null || forceSync) {
            try {
                if (figure == null) {
                    log.info("DB에 정치인 정보가 없어 API에서 동기화: {}", figureName);
                    figure = figureApiService.syncFigureInfoByName(figureName);
                } else {
                    log.info("정치인 정보 강제 동기화: {}", figureName);
//                    figureApiService.updateFigureInfoByName(figureName);
                }
                if (syncStatements) {
                    log.info("발언 정보도 함께 동기화: {}", figureName);
                    syncService.syncStatementsByFigure(figureName);
                }
            } catch (Exception e) {
                log.error("정치인 정보 동기화 실패: {} - {}", figureName, e.getMessage());
                if (figure == null) {
                    throw new EntityNotFoundException("해당 정치인을 찾을 수 없습니다: " + figureName);
                }
            }
        }
        return figure;
    }

    /**
     * 정치인 존재 확인 및 필요시 동기화 (발언 정보 포함)
     * @param figureName 정치인 이름
     * @param forceSync 강제 동기화 여부
     * @return Figure 엔티티
     */
    public Figure ensureFigureExists(String figureName, boolean forceSync) {
        return ensureFigureExists(figureName, forceSync, true);
    }

    /**
     * 정치인 존재 확인 (동기화 없음)
     * @param figureName 정치인 이름
     * @return Figure 엔티티
     */
    public Figure ensureFigureExists(String figureName) {
        return ensureFigureExists(figureName, false, false);
    }
}

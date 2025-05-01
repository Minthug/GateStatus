package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.request.FindFigureCommand;
import com.example.GateStatus.domain.figure.service.request.RegisterFigureCommand;
import com.example.GateStatus.domain.figure.service.request.UpdateFigureCommand;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.figure.service.response.RegisterFigureResponse;
import com.example.GateStatus.domain.figure.service.response.UpdateFigureResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureService {

    private final FigureRepository figureRepository;
    private final FigureApiService figureApiService;
    private final FigureCacheService figureCacheService;
    private final CacheManager cacheManager;

    /**
     * 국회의원 등록 또는 조회
     * @param command
     * @return
     */
    @Transactional
    public RegisterFigureResponse getRegisterFigure(final RegisterFigureCommand command) {
        Figure findFigure = figureRepository.findByName(command.name())
                .orElseGet(() -> {
                    log.info("신규 국회의원 등록: {}", command.name());
                    Figure figure = Figure.builder()
                            .name(command.name())
                            .englishName(command.englishName())
                            .birth(command.birth())
                            .constituency(command.constituency())
                            .profileUrl(command.profileUrl())
                            .figureType(command.figureType())
                            .education(command.education())
                            .careers(command.careers())
                            .sites(command.sites())
                            .activities(command.activities())
                            .updateSource(command.updateSource())
                            .build();
                    Figure savedFigure = figureRepository.save(figure);
                    if (savedFigure.getFigureId() != null) {
                        figureCacheService.updateFigureCache(savedFigure);
                    }

                    return savedFigure;
                });
        return RegisterFigureResponse.from(findFigure);
    }

    /**
     * 국회의원 상세 정보 조회
     * @param command
     * @return
     */
    @Transactional(readOnly = true)
    public FindFigureDetailResponse findFigure(FindFigureCommand command) {
        if (command.figureId() != null || command.figureId().isEmpty()) {
            throw new IllegalArgumentException("국회의원 ID는 필수 값입니다");
        }

        log.debug("조회 시도 figureId: {}", command.figureId());

        try {
            Figure findFigure = figureCacheService.findFigureById(command.figureId());
            return FindFigureDetailResponse.from(findFigure);
        } catch (EntityNotFoundException e) {
            log.warn("국회의원 정보 조회 실패: {}", command.figureId());
            throw e;
        }
    }

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
    @Transactional(readOnly = true)
    public FigureDTO findFigureById(String figureId) {
        if (figureId == null || figureId.isEmpty()) {
            throw new IllegalArgumentException("국회의원 ID는 필수 값입니다");
        }

        try {
            return figureCacheService.findFigureDtoById(figureId);
        } catch (EntityNotFoundException e) {
            log.warn("국회의원 정보 조회 실패: {}", figureId);
            throw e;
        }
    }

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

    @Transactional
    public UpdateFigureResponse updateFigure(String figureId, UpdateFigureCommand command) {
        if (figureId == null || figureId.isEmpty()) {
            throw new IllegalArgumentException("국회의원 ID는 필수 값 입니다");
        }

        log.info("국회의원 정보 업데이트: {}", figureId);
        Figure figure = figureRepository.findByFigureId(figureId)
                .orElseThrow(() -> new NotFoundFigureException("국회의원을 찾을 수 없습니다" + figureId));

        figure.update(
                command.name(),
                command.englishName(),
                command.birth(),
                command.constituency(), // place 대신 constituency 사용
                command.profileUrl(),
                command.figureType(),
                command.figureParty(),
                command.education(),
                command.careers(),
                command.sites(),
                command.activities(),
                command.updateSource()
        );

        Figure updateFigure = figureRepository.save(figure);

        figureCacheService.updateFigureCache(updateFigure);
        log.debug("국회의원 정보 업데이트 완료: {}", updateFigure.getName());

        return UpdateFigureResponse.from(updateFigure);
    }

//    @CacheEvict(value = {"figures", "figure-dtos"}, key = "#figure.figureId")
//    public void updateCache(Figure figure) {
//        log.info("Cache evicted for figure ID: {}", figure.getFigureId());
//    }


    @Transactional
    public void deleteFigure(String figureId) {
        if (figureId == null || figureId.isEmpty()) {
            throw new IllegalArgumentException("국회의원 ID는 필수 값입니다");
        }

        log.info("국회의원 정보 삭제: {}", figureId);
        Figure figure = figureRepository.findByFigureId(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 국회의원을 찾을 수 없습니다: " + figureId));

        figureRepository.delete(figure);
        figureCacheService.evictFigureCache(figureId);
    }

    @Transactional(readOnly = true)
    public List<FindFigureDetailResponse> getPopularFigures(int limit) {
        log.debug("인기 국회의원 조회: limit={}", limit);
        return figureCacheService.getPopularFigures(limit)
                .stream()
                .map(FindFigureDetailResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * API에서 국회의원 정보 동기화
     */
    @Transactional
    public Figure syncFromApi(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("국회의원 이름은 필수 값입니다");
        }

        log.info("API에서 국회의원 정보 동기화: {}", name);
        return figureApiService.syncFigureInfoByName(name);
    }

//    /**
//     * 모든 캐시 초기화
//     */
//    public void clearAllCaches() {
//        log.info("모든 국회의원 캐시 초기화");
//        figureCacheService.clearAllCaches();
//    }
}

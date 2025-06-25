package com.example.GateStatus.domain.figure.controller;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.service.core.FigureCacheService;
import com.example.GateStatus.domain.figure.service.core.FigureService;
import com.example.GateStatus.domain.figure.service.external.FigureApiService;
import com.example.GateStatus.domain.figure.service.external.FigureSyncService;
import com.example.GateStatus.domain.figure.service.request.FigureSearchRequest;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/figures")
@Slf4j
public class FigureController {

    private final FigureService figureService;
    private final FigureSyncService syncService;
    private final FigureApiService apiService;
    private final FigureCacheService cacheService;
    private final CacheManager cacheManager;
    private final VoteService voteService;


    @GetMapping("/{figureId}")
    public ResponseEntity<Figure> findFigure(@PathVariable String figureId) {
        log.info("국회의원 조회 요청: {}", figureId);

        try {
            Cache cache = cacheManager.getCache("figure-dtos");
            Cache.ValueWrapper value = cache != null ? cache.get(figureId) : null;
            log.info("캐시 상태: {}", value != null ? "hit" : "miss");
        } catch (Exception e) {
            log.warn("캐시 확인 중 오류: {}", e.getMessage());
        }

        try {
            FigureDTO figureDTO = figureService.getFigure(figureId);
            Figure figure = convertDtoToEntity(figureDTO);

            return ResponseEntity.ok(figure);
        } catch (NotFoundFigureException e) {
            log.warn("국회의원 조회 실패: {}", figureId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("국회의원 조회 중 오류: {}", figureId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<FindFigureDetailResponse>> findAllFigures(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size,  // 기본값 20으로 수정
                                                                         @RequestParam(required = false) String type,
                                                                         @RequestParam(required = false) String keyword) {
        log.info("국회의원 목록 조회: page={}, size={}, type={}, keyword={}", page, size, type, keyword);

        try {
            FigureSearchRequest request = FigureSearchRequest.builder()
                    .keyword(keyword)
                    .searchType(type != null ? type : "all")
                    .page(page)
                    .size(size)
                    .sortBy("name")
                    .sortDirection("ASC")
                    .build();

           Page<FigureDTO> figureDTOs = figureService.getFigures(request);

           Page<FindFigureDetailResponse> responses = figureDTOs.map(dto -> {
               Figure figure = convertDtoToEntity(dto);
               return FindFigureDetailResponse.from(figure);
           });

           return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("국회의원 목록 조회 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    private Figure convertDtoToEntity(FigureDTO dto) {
        return Figure.builder()
                .id(null) // ID는 DTO에 없으므로 null
                .figureId(dto.getFigureId())
                .name(dto.getName())
                .englishName(dto.getEnglishName())
                .birth(dto.getBirth())
                .constituency(dto.getConstituency())
                .profileUrl(dto.getProfileUrl())
                .figureType(dto.getFigureType())
                .figureParty(dto.getFigureParty())
                .education(dto.getEducation())
                .careers(dto.getCareers().stream()
                        .map(careerDTO -> Career.builder()
                                .title(careerDTO.getTitle())
                                .position(careerDTO.getPosition())
                                .organization(careerDTO.getOrganization())
                                .period(careerDTO.getPeriod())
                                .build())
                        .collect(Collectors.toList()))
                .sites(dto.getSites())
                .activities(dto.getActivities())
                .viewCount(dto.getViewCount())
                .build();
    }
}

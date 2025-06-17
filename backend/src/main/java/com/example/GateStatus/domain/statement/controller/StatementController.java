package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.figure.service.FigureService;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.*;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.domain.statement.service.response.StatementSearchCriteria;
import com.example.GateStatus.global.config.open.ApiResponse;
import com.example.GateStatus.global.config.redis.RedisCacheService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/statements")
@RequiredArgsConstructor
@Slf4j
public class StatementController {

    private final StatementService statementService;
    private final StatementApiService apiService;
    private final StatementApiMapper apiMapper;
    private final RedisCacheService cacheService;
    private final StatementSyncService statementSyncService;
    private final FigureRepository figureRepository;
    private final FigureApiService figureApiService;
    private final StatementRelevanceService relevanceService;
    private final FigureService figureService;

    // ==================== 핵심 CRUD ====================

    /**
     * 발언 ID로 발언 상세 정보 조회
     * @param id
     * @return
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<StatementResponse> getStatementById(@PathVariable String id) {
        log.info("발언 상세 정보 조회 요청: {}", id);
        StatementResponse statement = statementService.findStatementById(id);
        return ResponseEntity.ok(statement);
    }

    // ==================== 통합 검색 (하나로 합침) ====================

    @GetMapping("/search")
    public ResponseEntity<Page<StatementResponse>> searchStatements(@RequestParam String keyword,
                                                                    @PageableDefault(size = 10) Pageable pageable) {
        StatementSearchCriteria criteria = StatementSearchCriteria.keyword(keyword);
        Page<StatementResponse> statements = statementService.searchStatements(criteria, pageable);

        return ResponseEntity.ok(statements);
    }

    /**
     * 통합 검색 엔드포인트 - 모든 필드에서 검색
     * GET /v1/statements/search/advanced?keyword=경제&type=ASSEMBLY_SPEECH&startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<Page<StatementResponse>> searchAdvanced(@RequestParam(defaultValue = "FULL_TEXT") SearchType searchType,
                                                                  @RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) String exactPhrase,
                                                                  @RequestParam(required = false) List<String> keywords,
                                                                  @RequestParam(required = false) StatementType type,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                  @RequestParam(required = false) String source,
                                                                  @RequestParam(required = false, defaultValue = "50") Integer limit,
                                                                  @PageableDefault(size = 10) Pageable pageable) {

        log.info("고급 검색: keyword={}, exactPhrase={}, type={}", keyword, exactPhrase, type);

        StatementSearchCriteria criteria = StatementSearchCriteria.fromParams(
                keyword, exactPhrase, keywords, type, startDate, endDate, source, limit
        );

        Page<StatementResponse> statements = statementService.searchStatements(criteria, pageable);
        return ResponseEntity.ok(statements);
    }

    // ==================== 정치인별 조회 (통합) ====================


    /**
     * 특정 정치인의 발언 목록 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @GetMapping("/figure/{figureId}")
    public ResponseEntity<Page<StatementResponse>> getStatementsByFigure(@PathVariable Long figureId,
                                                                         @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인별 발언 목록 조회 요청: {}", figureId);
        Page<StatementResponse> statements = statementService.findStatementsByFigure(figureId, pageable);
        return ResponseEntity.ok(statements);
    }




    @GetMapping("/by-name")
    public ResponseEntity<?> getStatementsByFigureName(@RequestParam String figureName,
                                                       @RequestParam(required = false, defaultValue = "false") Boolean sync,
                                                       @PageableDefault(size = 10) Pageable pageable) {
        log.info("국회의원 이름으로 발언 목록 조회 요청: {}, 동기화 여부: {}", figureName, sync);

        try {
            Figure figure = figureService.ensureFigureExists(figureName, sync);

            Page<StatementResponse> statements = statementService.findStatementsByFigure(figure.getId(), pageable);

            return ResponseEntity.ok(statements);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("발언 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("발언 목록 조회 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/by-relevant-name")
    public ResponseEntity<Page<StatementResponse>> getRelevantStatementsByFigureName(@RequestParam String figureName,
                                                                                     @RequestParam(required = false, defaultValue = "false") Boolean sync,
                                                                                     @PageableDefault(size = 10) Pageable pageable) {
        log.info("인물 '{}' 관련성 높은 발언 조회 요청, 동기화 여부: {}", figureName, sync);

        try {
            Figure figure = figureService.ensureFigureExists(figureName, sync);

            Page<StatementResponse> statements = relevanceService.findStatementsByRelevance(figureName, pageable);
            log.info("관련성 기준 발언 조회 완료: {}건", statements.getTotalElements());

            return ResponseEntity.ok(statements);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("관련 발언 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== PathVariable 적절한 사용 (고유 식별자) ====================

    /**
     * 특정 유형의 발언 목록 조회
     * @param type
     * @return
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<StatementResponse>> getStatementsByType(@PathVariable StatementType type) {
        log.info("유형별 발언 목록 조회 요청: {}", type);
        List<StatementResponse> statements = statementService.findStatementsByType(type);
        return ResponseEntity.ok(statements);
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<StatementResponse>> getStatementsBySource(@PathVariable String source) {
        log.info("출처별 발언 목록 조회 요청: {}", source);
        List<StatementResponse> statements = statementService.findStatementsBySource(source);
        return ResponseEntity.ok(statements);
    }

    // ==================== API 기반 검색 (PathVariable 사용 - 명확한 식별) ====================

//    @GetMapping("/search/figure/{figureName}")
//    public ResponseEntity<List<StatementApiDTO>> getStatementsByFigure(@PathVariable String figureName) {
//        log.info("정치인 발언 직접 조회 요청: {}", figureName);
//
//        String cacheKey = "direct:statements:figure:" + figureName;
//
//        List<StatementApiDTO> statements = cacheService.getOrSet(cacheKey, () -> {
//                    AssemblyApiResponse<String> apiResponse = statementSyncService.fetchStatementsByFigure(figureName);
//                    if (!apiResponse.isSuccess()) {
//                        log.error("API 호출 실패: {}", apiResponse.resultMessage());
//                        return List.of();
//                    }
//                    return apiMapper.map(apiResponse);
//                },
//                300
//        );
//        return ResponseEntity.ok(statements);
//    }

}


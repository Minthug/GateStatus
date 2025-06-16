package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.figure.service.FigureService;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.*;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.global.config.open.ApiResponse;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.example.GateStatus.global.config.redis.RedisCacheService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.swing.plaf.nimbus.State;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 통합 검색 엔드포인트 - 모든 필드에서 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @GetMapping("/search")
    public ResponseEntity<Page<StatementResponse>> searchStatements(@RequestParam String keyword,
                                                                    @PageableDefault(size = 10) Pageable pageable) {
        log.info("[MongoDB] 발언 통합 검색 요청: 키워드 = {}", keyword);
        Page<StatementResponse> statements = statementService.searchStatements(keyword, pageable);
        return ResponseEntity.ok(statements);
    }

    /**
     * 발언 내용 검색 - 정규식 기반 검색으로 유연한 검색 가능
     * @param keyword
     * @return
     */
    @GetMapping("/search/content")
    public ResponseEntity<List<StatementResponse>> searchContent(@RequestParam String keyword) {
        log.info("[MongoDB] 발언 내용 검색 요청: 키워드 = {}", keyword);
        List<StatementResponse> responses = statementService.searchStatementContent(keyword);
        return ResponseEntity.ok(responses);
    }

    /**
     * 정확한 문구 검색 - 완전히 일치하는 문구 검색
     * @param phrase
     * @return
     */
    @GetMapping("/search/exact")
    public ResponseEntity<List<StatementResponse>> searchExactPhrase(@RequestParam String phrase) {
        log.info("[MongoDB] 정확한 문구 검색 요청: 문구 = {}", phrase);
        List<StatementResponse> responses = statementService.searchExtractPhrase(phrase);
        return ResponseEntity.ok(responses);
    }

    /**
     * 다중 키워드 검색 - 모든 키워드를 포함하는 발언 검색 (AND 조건)
     * @param keywords
     * @return
     */
    @GetMapping("/search/multi")
    public ResponseEntity<List<StatementResponse>> searchMultiKeywords(@RequestParam List<String> keywords) {
        log.info("[MongoDB] 다중 키워드 검색 요청: 키워드 = {}", keywords);
        List<StatementResponse> responses = statementService.searchWithMultipleKeywords(keywords);
        return ResponseEntity.ok(responses);
    }

    /**
     * 최근 발언 중 키워드 검색
     * @param keyword
     * @param limit
     * @return
     */
    @GetMapping("/search/recent")
    public ResponseEntity<List<StatementResponse>> searchRecentStatements(@RequestParam String keyword,
                                                                          @RequestParam(defaultValue = "10") int limit) {
        log.info("[MongoDB] 최근 발언 검색 요청: 키워드 = {}, 제한 = {}", keyword, limit);
        List<StatementResponse> responses = statementService.searchRecentStatements(keyword, limit);
        return ResponseEntity.ok(responses);
    }

    /**
     * 발언 길이 기준 검색 - 긴 발언
     * @param minLength
     * @param size
     * @return
     */
    @GetMapping("/search/long")
    public ResponseEntity<List<StatementResponse>> searchLongStatements(@RequestParam(defaultValue = "500") int minLength,
                                                                        @RequestParam(defaultValue = "10") int size) {
        log.info("[MongoDB] 긴 발언 검색 요청: 최소 길이 = {}", minLength);
        List<StatementResponse> responses = statementService.findLongStatements(minLength, PageRequest.of(0, size));
        return ResponseEntity.ok(responses);
    }

    /**
     * 발언 길이 기준 검색 - 짧은 발언
     * @param maxLength
     * @param size
     * @return
     */
    @GetMapping("/search/short")
    public ResponseEntity<List<StatementResponse>> searchShortStatements(@RequestParam(defaultValue = "100") int maxLength,
                                                                         @RequestParam(defaultValue = "10") int size) {
        log.info("[MongoDB] 짧은 발언 검색 요청: 최대 길이 = {}", maxLength);
        List<StatementResponse> responses = statementService.findShortStatements(maxLength, PageRequest.of(0, size));
        return ResponseEntity.ok(responses);
    }

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

    /**(
     * 기간별 발언 목록 조회
     * @param startDate
     * @param endDate
     * @return
     */
    @GetMapping("/period")
    public ResponseEntity<List<StatementResponse>> getStatementsByPeriod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("기간별 발언 목록 조회 요청: {} ~ {}", startDate, endDate);
        List<StatementResponse> statements = statementService.findStatementsByPeriod(startDate, endDate);
        return ResponseEntity.ok(statements);
    }

    @GetMapping("/search/figure/{figureName}")
    public ResponseEntity<List<StatementApiDTO>> getStatementsByFigure(@PathVariable String figureName) {
        log.info("정치인 발언 직접 조회 요청: {}", figureName);

        String cacheKey = "direct:statements:figure:" + figureName;

        List<StatementApiDTO> statements = cacheService.getOrSet(cacheKey, () -> {
                        AssemblyApiResponse<String> apiResponse = statementSyncService.fetchStatementsByFigure(figureName);
                        if (!apiResponse.isSuccess()) {
                            log.error("API 호출 실패: {}", apiResponse.resultMessage());
                            return List.of();
                        }
                        return apiMapper.map(apiResponse);
                },
                300
        );
        return ResponseEntity.ok(statements);
    }

    @PostMapping("/sync/figureName")
    public ResponseEntity<ApiResponse<Integer>> syncStatementsByFigure(@RequestParam String figureName) {
        log.info("국회의원 '{}' 발언 정보 동기화 요청", figureName);

        try {
            int count = statementService.syncStatementsByFigure(figureName);
            return ResponseEntity.ok(ApiResponse.success(String.format("국회의원 '%s' 발언 정보 %d건 동기화 완료", figureName, count), count));
        } catch (Exception e) {
            log.error("발언 정보 동기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("발언 정보 동기화 실패: " + e.getMessage()));
        }
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

    @GetMapping("/relevant/{figureName}")
    public ResponseEntity<?> getRelevantStatementsByFigureName(@PathVariable String figureName,
                                                               @RequestParam(required = false, defaultValue = "false") Boolean sync,
                                                               @PageableDefault(size = 10) Pageable pageable) {
        log.info("인물 '{}' 관련성 높은 발언 조회 요청, 동기화 여부: {}", figureName, sync);

        try {

            // 1. 이름으로 국회의원 찾기
            Figure figure = figureRepository.findByName(figureName)
                    .orElse(null);

            // 2. DB에 없거나 동기화 요청이 있으면 API에서 동기화
            if (figure == null || Boolean.TRUE.equals(sync)) {
                try {
                    log.info("국회의원 정보 동기화 시도: {}", figureName);
                    if (figure == null) {
                        figure = figureApiService.syncFigureInfoByName(figureName);
                    }

                    // 발언 정보도 함께 동기화
                    statementService.syncStatementsByFigure(figureName);
                } catch (Exception e) {
                    log.warn("동기화 실패: {} - {}", figureName, e.getMessage());
                    if (figure == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("해당 국회의원을 찾을 수 없습니다: " + figureName));
                    }
                }
            }

            // 3. 관련성 기준으로 발언 조회
            Page<StatementResponse> statements = relevanceService.findStatementsByRelevance(figureName, pageable);

            // 4. 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("figureName", figureName);
            response.put("figureId", figure.getId());
            response.put("totalStatements", statements.getTotalElements());
            response.put("statements", statements.getContent());
            response.put("totalPages", statements.getTotalPages());
            response.put("currentPage", statements.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("관련 발언 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("발언 조회 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/all")
    public ResponseEntity<ApiResponse<String>> syncAllStatementsAsync() {
        log.info("모든 국회의원 발언 정보 비동기 동기화 요청");

        String jobId = statementSyncService.syncStatementsAsync();
        return ResponseEntity.ok(ApiResponse.success("모든 국회의원 발언 정보 비동기 동기화 작업이 시작되었습니다", jobId));
    }

    @GetMapping("/sync/status/{jobId}")
    public ResponseEntity<ApiResponse<SyncJobStatus>> getSyncStatus(@PathVariable String jobId) {
        SyncJobStatus status = statementSyncService.getSyncJobStatus(jobId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success("발언 정보 동기화 작업 상태", status));
    }

    @PostMapping("/sync/period")
    public ResponseEntity<ApiResponse<Integer>> syncStatementsByPeriod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("기간별 발언 정보 동기화 요청: {} ~ {}", startDate, endDate);

        try {
            int count = statementSyncService.syncStatementsByPeriod(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(String.format("기간(%s ~ %s) 발언 정보 %d건 동기화 완료",
                    startDate, endDate, count), count));
        } catch (Exception e) {
            log.error("기간별 발언 정보 동기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("기간별 발언 정보 동기화 실패: " + e.getMessage()));
        }
    }
}


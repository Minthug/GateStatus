package com.example.GateStatus.domain.figure.controller;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.core.FigureService;
import com.example.GateStatus.domain.figure.service.core.FigureCacheService;
import com.example.GateStatus.domain.figure.service.external.FigureApiService;
import com.example.GateStatus.domain.figure.service.response.*;
import com.example.GateStatus.domain.vote.service.VoteService;
import com.example.GateStatus.global.config.open.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/figures")
@Slf4j
public class FigureController {

    private final FigureService figureService;
    private final FigureApiService figureApiService;
    private final FigureCacheService cacheService;
    private final CacheManager cacheManager;
    private VoteService voteService;

//    @PostMapping
//    public ResponseEntity<RegisterFigureResponse> registerFigure(@RequestBody RegisterFigureCommand command) {
//        RegisterFigureResponse response = figureService.getRegisterFigure(command);
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/{figureId}")
    public ResponseEntity<Figure> findFigure(@PathVariable String figureId) {
        log.info("국회의원 조회 요청: {}", figureId);

        // 캐시 상태 확인
        try {
            Cache cache = cacheManager.getCache("figure-dtos");
            Cache.ValueWrapper value = cache != null ? cache.get(figureId) : null;
            log.info("캐시 상태: {}", value != null ? "hit" : "miss");
        } catch (Exception e) {
            log.warn("캐시 확인 중 오류: {}", e.getMessage());
        }


        Figure figure = cacheService.findFigureById(figureId);
        return ResponseEntity.ok(figure);
    }

    @GetMapping
    public ResponseEntity<Page<FindFigureDetailResponse>> findAllFigures(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "0") int size,
                                                                         @RequestParam(required = false) String type,
                                                                         @RequestParam(required = false) String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(figureService.findAllFigures(pageRequest, type, keyword));
    }

    @GetMapping("/type/{figureId}")
    public ResponseEntity<List<FindFigureDetailResponse>> findFiguresByType(@PathVariable FigureType figureType) {
        return ResponseEntity.ok(figureService.findFiguresByType(figureType));
    }

//    @PatchMapping("/{figureId}")
//    public ResponseEntity<UpdateFigureResponse> updateFigure(@PathVariable String figureId,
//                                                             @RequestBody UpdateFigureRequest request) {
//        try {
//            UpdateFigureCommand command = UpdateFigureCommand.from(request);
//            // 서비스 메서드 호출하여 업데이트 수행
//            UpdateFigureResponse response = cacheService.updateFigure(figureId, command);
//
//            return ResponseEntity.ok(response);
//        } catch (EntityNotFoundException e) {
//            log.warn("국회의원 정보 업데이트 실패: {} - {}", figureId, e.getMessage());
//            return ResponseEntity.notFound().build();
//        } catch (IllegalArgumentException e) {
//            log.warn("국회의원 정보 업데이트 유효성 검사 실패: {} - {}", figureId, e.getMessage());
//            return ResponseEntity.badRequest().build();
//        } catch (Exception e) {
//            log.error("국회의원 정보 업데이트 중 예상치 못한 오류: {} - {}", figureId, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }


//    @PostMapping("/sync")
////    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<FindFigureDetailResponse> syncFigureByName(@RequestBody SyncFigureRequest request) {
//        Figure figure = figureApiService.syncFigureInfoByName(request.name());
//        return ResponseEntity.ok(FindFigureDetailResponse.from(figure));
//    }

    @PostMapping("/sync/all")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> syncAllFigures() {
        log.info("모든 국회의원 정보 동기화 요청");

        try {
            int count = figureApiService.syncAllFiguresV3();

            return ResponseEntity.ok(ApiResponse.success("국회의원 정보 동기화 완료", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("국회의원 정보 동기화 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/allV2")
    public ResponseEntity<ApiResponse<String>> syncFiguresAsync() {
        String jobId = figureApiService.syncAllFiguresV4();
        return ResponseEntity.ok(ApiResponse.success("국회의원 정보 비동기 동기화 작업이 시작되었습니다", jobId));
    }

//    @GetMapping("/sync/status/{jobId}")
//    public ResponseEntity<ApiResponse<SyncJobStatus>> getSyncStatus(@PathVariable String jobId) {
//
//        SyncJobStatus status = figureApiService.getSyncJobStatus(jobId);
//
//        if (status == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.ok(ApiResponse.success("국회의원 정보 동기화 작업 상태", status));
//    }

    @PostMapping("/sync/party")
    public ResponseEntity<SyncPartyResponse> syncFiguresByParty(@RequestParam String partyName) {
        int syncedCount = figureApiService.syncFigureByParty(partyName);
        return ResponseEntity.ok(new SyncPartyResponse(partyName, syncedCount));
    }

    // FigureController 클래스에 추가
    @GetMapping("/search/name")
    public ResponseEntity<?> searchFigureByName(@RequestParam String name) {
        log.info("이름으로 국회의원 검색 요청: {}", name);
        FigureDTO figure = figureService.findFigureByName(name);

        if (figure != null) {
            return ResponseEntity.ok(figure);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

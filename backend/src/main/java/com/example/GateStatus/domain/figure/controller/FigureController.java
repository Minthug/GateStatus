package com.example.GateStatus.domain.figure.controller;

import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.figure.service.FigureCacheService;
import com.example.GateStatus.domain.figure.service.FigureService;
import com.example.GateStatus.domain.figure.service.request.RegisterFigureCommand;
import com.example.GateStatus.domain.figure.service.request.UpdateFigureCommand;
import com.example.GateStatus.domain.figure.service.request.UpdateFigureRequest;
import com.example.GateStatus.domain.figure.service.response.*;
import com.example.GateStatus.global.config.open.ApiResponse;
import com.example.GateStatus.global.config.redis.CacheConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private final CacheManager cacheManager;

    @PostMapping
    public ResponseEntity<RegisterFigureResponse> registerFigure(@RequestBody RegisterFigureCommand command) {
        RegisterFigureResponse response = figureService.getRegisterFigure(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{figureId}")
    public ResponseEntity<FigureDTO> findFigure(@PathVariable String figureId) {
        log.info("국회의원 조회 요청: {}", figureId);

        // 캐시 상태 확인
        try {
            Cache cache = cacheManager.getCache("figure-dtos");
            Cache.ValueWrapper value = cache != null ? cache.get(figureId) : null;
            log.info("캐시 상태: {}", value != null ? "hit" : "miss");
        } catch (Exception e) {
            log.warn("캐시 확인 중 오류: {}", e.getMessage());
        }


        FigureDTO figure = figureService.findFigureById(figureId);
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

    @GetMapping("/v1/type/{figureId}")
    public ResponseEntity<List<FindFigureDetailResponse>> findFiguresByType(@PathVariable FigureType figureType) {
        return ResponseEntity.ok(figureService.findFiguresByType(figureType));
    }

    @PatchMapping("/{figureId}")
    public ResponseEntity<UpdateFigureResponse> updateFigure(@PathVariable String figureId,
                                                             @RequestBody UpdateFigureRequest request) {
        try {
            UpdateFigureCommand command = UpdateFigureCommand.from(request);
            // 서비스 메서드 호출하여 업데이트 수행
            UpdateFigureResponse response = figureService.updateFigure(figureId, command);

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("국회의원 정보 업데이트 실패: {} - {}", figureId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("국회의원 정보 업데이트 유효성 검사 실패: {} - {}", figureId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("국회의원 정보 업데이트 중 예상치 못한 오류: {} - {}", figureId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 국회의원 정보 업데이트 폼 데이터 조회
     * 기존 정보를 수정 폼에 표시하기 위한 API
     * @param figureId 국회의원 ID
     * @return 국회의원 정보가 담긴 업데이트 요청 객체
     */
    @GetMapping("/{figureId}/edit")
    public ResponseEntity<UpdateFigureRequest> getUpdateForm(@PathVariable String figureId) {
        try {
            // 국회의원 정보 조회
            FigureDTO figureDTO = figureService.findFigureById(figureId);

            // DTO를 UpdateFigureRequest로 변환
            UpdateFigureRequest formData = UpdateFigureRequest.fromDto(figureDTO);

            return ResponseEntity.ok(formData);
        } catch (EntityNotFoundException e) {
            log.warn("국회의원 정보 폼 조회 실패: {} - {}", figureId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("국회의원 정보 폼 조회 중 오류: {} - {}", figureId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{figureId}")
    public ResponseEntity<Void> deleteFigure(@PathVariable String figureId) {
        figureService.deleteFigure(figureId);
        return ResponseEntity.noContent().build();
    }


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

    @PostMapping("/sync/party")
    public ResponseEntity<SyncPartyResponse> syncFiguresByParty(@RequestParam String partyName) {
        int syncedCount = figureApiService.syncFigureByParty(partyName);
        return ResponseEntity.ok(new SyncPartyResponse(partyName, syncedCount));
    }
}

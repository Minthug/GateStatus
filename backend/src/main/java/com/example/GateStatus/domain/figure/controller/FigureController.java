package com.example.GateStatus.domain.figure.controller;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.figure.service.FigureCacheService;
import com.example.GateStatus.domain.figure.service.FigureService;
import com.example.GateStatus.domain.figure.service.request.RegisterFigureCommand;
import com.example.GateStatus.domain.figure.service.request.SyncFigureRequest;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.figure.service.response.RegisterFigureResponse;
import com.example.GateStatus.domain.figure.service.response.SyncPartyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/figures")
@Slf4j
public class FigureController {

    private final FigureService figureService;
    private final FigureApiService figureApiService;
    private final FigureCacheService figureCacheService;

    @PostMapping
    public ResponseEntity<RegisterFigureResponse> registerFigure(@RequestBody RegisterFigureCommand command) {
        RegisterFigureResponse response = figureService.getRegisterFigure(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{figureId}")
    public ResponseEntity<FigureDTO> findFigure(@PathVariable String figureId) {
        log.info("국회의원 조회 요청: {}", figureId);

        FigureDTO figure = figureCacheService.findFigureDtoById(figureId);
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

//    @PatchMapping("/{figureId}")
//    public ResponseEntity<UpdateFigureResponse> updateFigure(@PathVariable String figureId) {
//        UpdateFigureResponse response = figureService.updateCache(figureId);
//        return ResponseEntity.ok(response);
//    }

//    @DeleteMapping("/{figureId}")
//    public ResponseEntity<Void> deleteFigure(@PathVariable String figureId) {
//        figureService.deleteFigure(figureId);
//        return ResponseEntity.noContent().build();
//    }


    @PostMapping("/sync")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FindFigureDetailResponse> syncFigureByName(@RequestBody SyncFigureRequest request) {
        Figure figure = figureApiService.syncFigureInfoByName(request.name());
        return ResponseEntity.ok(FindFigureDetailResponse.from(figure));
    }

    @PostMapping("/sync/all")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> syncAllFigures() {
        int count = figureApiService.syncAllFigureV2();
        return ResponseEntity.ok(Map.of("syncCount", count));
    }

    @PostMapping("/sync/party")
    public ResponseEntity<SyncPartyResponse> syncFiguresByParty(@RequestParam String partyName) {
        int syncedCount = figureApiService.syncFigureByParty(partyName);
        return ResponseEntity.ok(new SyncPartyResponse(partyName, syncedCount));
    }
}

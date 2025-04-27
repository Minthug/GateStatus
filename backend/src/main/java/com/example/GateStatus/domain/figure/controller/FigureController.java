package com.example.GateStatus.domain.figure.controller;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.figure.service.FigureService;
import com.example.GateStatus.domain.figure.service.request.FindFigureCommand;
import com.example.GateStatus.domain.figure.service.request.RegisterFigureCommand;
import com.example.GateStatus.domain.figure.service.request.UpdateFigureCommand;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.figure.service.response.RegisterFigureResponse;
import com.example.GateStatus.domain.figure.service.response.UpdateFigureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/figures")
public class FigureController {

    private final FigureService figureService;
    private final FigureApiService figureApiService;

    @PostMapping
    public ResponseEntity<RegisterFigureResponse> registerFigure(@RequestBody RegisterFigureCommand command) {
        RegisterFigureResponse response = figureService.getRegisterFigure(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{figureId}")
    public ResponseEntity<FindFigureDetailResponse> findFigure(@PathVariable Long figureId) {
        FindFigureDetailResponse response = figureService.findFigure(FindFigureCommand.of(figureId));
        return ResponseEntity.ok(response);
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
    public ResponseEntity<UpdateFigureResponse> updateFigure(@PathVariable Long figureId,
                                                             @RequestBody UpdateFigureCommand figureCommand) {
        UpdateFigureResponse response = figureService.updateFigure(figureId, figureCommand);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{figureId}")
    public ResponseEntity<Void> deleteFigure(@PathVariable Long figureId) {
        figureService.deleteFigure(figureId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/sync/{name}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FindFigureDetailResponse> syncFigureByName(@PathVariable String name) {
        Figure figure = figureApiService.syncFigureInfoByName(name);
        return ResponseEntity.ok(FindFigureDetailResponse.from(figure));
    }

    @PostMapping("/sync/all")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> syncAllFigures() {
        int count = figureApiService.syncAllFigures();
        return ResponseEntity.ok(Map.of("syncCount", count));
    }
}

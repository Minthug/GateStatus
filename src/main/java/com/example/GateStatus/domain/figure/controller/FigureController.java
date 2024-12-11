package com.example.GateStatus.domain.figure.controller;

import com.example.GateStatus.domain.figure.service.FigureService;
import com.example.GateStatus.domain.figure.service.request.FindFigureCommand;
import com.example.GateStatus.domain.figure.service.request.RegisterFigureCommand;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.figure.service.response.RegisterFigureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/figures")
public class FigureController {

    private final FigureService figureService;

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
}

package com.example.GateStatus.domain.figure.controller;

import com.example.GateStatus.domain.figure.service.core.FigureCacheService;
import com.example.GateStatus.domain.figure.service.core.FigureService;
import com.example.GateStatus.domain.figure.service.external.FigureApiService;
import com.example.GateStatus.domain.figure.service.external.FigureSyncService;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.vote.service.VoteService;
import com.example.GateStatus.global.config.open.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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



    @GetMapping
    public ResponseEntity<ApiResponse<FigureDTO>> getFigure(@PathVariable @NotBlank(message = "국회의원 ID는 필수입니다.")) {

    }
}

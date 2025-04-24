package com.example.GateStatus.domain.comparison.controller;

import com.example.GateStatus.domain.comparison.service.ComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/compares")
@RequiredArgsConstructor
@Slf4j
public class ComparisonController {

    private final ComparisonService comparisonService;
}

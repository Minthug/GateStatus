package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureApiService {

    private final WebClient webClient;
    private final FigureApiMapper apiMapper;
    private final FigureRepository figureRepository;
}

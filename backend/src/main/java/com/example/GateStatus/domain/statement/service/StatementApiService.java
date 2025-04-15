package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.statement.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementApiService {

    private final WebClient.Builder webclientBuilder;
    private final StatementRepository statementRepository;
    private final FigureRepository figureRepository;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apikey;

}

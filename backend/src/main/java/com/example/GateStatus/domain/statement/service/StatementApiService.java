package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.statement.Statement;
import com.example.GateStatus.domain.statement.StatementType;
import com.example.GateStatus.domain.statement.repository.StatementRepository;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementApiService {

    private final WebClient.Builder webclientBuilder;
    private final StatementRepository statementRepository;
    private final FigureRepository figureRepository;
    private final StatementApiMapper statementApiMapper;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apikey;


    @Transactional
    public int syncStatementsByFigure(String figureName) {
        log.info("국회방송국 API에서 '{}' 인물 발언 정보 동기화 시작", figureName);

        Figure figure = figureRepository.findByName(figureName)
                .orElseThrow(() -> new EntityNotFoundException("해당 인물이 존재하지 않습니다: " + figureName));

        AssemblyApiResponse<String> apiResponse = fetchStatementsByFigure(figureName);
        if (!apiResponse.isSuccess()) {
            log.error("API 호출 실패: {}", apiResponse.resultMessage());
            throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
        }

        List<StatementApiDTO> apiDtos = statementApiMapper.map(apiResponse);
        int syncCount = 0;

        for (StatementApiDTO dto : apiDtos) {
            Optional<Statement> existingStatement = statementRepository.findAll().stream()
                    .filter(s -> s.getOriginalUrl().equals(dto.originalUrl()))
                    .findFirst();

            if (existingStatement.isPresent()) {
                log.debug("이미 존재하는 발언 건너: {}", dto.originalUrl());
                continue;
            }

            Statement statement = convertToStatement(dto, figure);
            statementRepository.save(statement);
            syncCount++;
        }

        log.info("'{}' 인물 발언 정보 동기화 완료. 총 {} 건 처리됨", figureName, syncCount);
        return syncCount;
    }

    @Transactional
    public int syncStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("국회방송국 API에서 기간({} ~ {}) 발언 정보 동기화 시작", startDate, endDate);

        AssemblyApiResponse<String> apiResponse = fetchStatementsByPeriod(startDate, endDate);
        if (!apiResponse.isSuccess()) {
            throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
        }

        List<StatementApiDTO> apiDtos = statementApiMapper.map(apiResponse);
        int syncCount = 0;

        for (StatementApiDTO dto : apiDtos) {
            Optional<Statement> existingStatement = statementRepository.findAll().stream()
                    .filter(s -> s.getOriginalUrl().equals(dto.originalUrl()))
                    .findFirst();

            if (existingStatement.isPresent()) {
                log.debug("이미 존재하는 발언 건너뜀: {}", dto.originalUrl());
                continue;
            }

            Figure figure = figureRepository.findByName(dto.figureName())
                    .orElseGet(() -> {
                        Figure newFigure = Figure.builder()
                                .name(dto.figureName())
                                .build();
                        return figureRepository.save(newFigure);
                    });

            Statement statement = convertToStatement(dto, figure);
            statementRepository.save(statement);
            syncCount++;
        }

        log.info("기간({} ~ {}) 발언 정보 동기화 완료. 총 {} 건 처리됨", startDate, endDate, syncCount);
        return syncCount;
    }

    private Statement convertToStatement(StatementApiDTO dto, Figure figure) {
        return Statement.builder()
                .figure(figure)
                .title(dto.title())
                .content(dto.content())
                .statementDate(dto.statementDate())
                .source(dto.source())
                .context(dto.context())
                .originalUrl(dto.originalUrl())
                .type(determineStatementType(dto.typeCode()))
                .build();
    }

    private StatementType determineStatementType(String typeCode) {
        switch (typeCode) {
            case "SPEECH":
                return StatementType.SPEECH;
            case "INTERVIEW":
                return StatementType.INTERVIEW;
            case "PRESS":
                return StatementType.PRESS_RELEASE;
            case "DEBATE":
                return StatementType.DEBATE;
            case "ASSEMBLY":
                return StatementType.ASSEMBLY_SPEECH;
            case "COMMITTEE":
                return StatementType.COMMITTEE_SPEECH;
            case "MEDIA":
                return StatementType.MEDIA_COMMENT;
            case "SNS":
                return StatementType.SOCIAL_MEDIA;
            default:
                return StatementType.OTHER;
        }
    }
}

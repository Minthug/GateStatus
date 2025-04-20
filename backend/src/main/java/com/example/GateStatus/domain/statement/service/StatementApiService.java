//package com.example.GateStatus.domain.statement.service;
//
//import com.example.GateStatus.domain.figure.Figure;
//import com.example.GateStatus.domain.figure.repository.FigureRepository;
//import com.example.GateStatus.domain.statement.entity.StatementType;
//import com.example.GateStatus.domain.statement.mongo.StatementDocument;
//import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
//import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
//import com.example.GateStatus.global.config.open.AssemblyApiResponse;
//import jakarta.persistence.EntityNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class StatementApiService {
//
//    private final WebClient.Builder webclientBuilder;
//    private final StatementMongoRepository statementRepository;
//    private final FigureRepository figureRepository;
//    private final StatementApiMapper statementApiMapper;
//
//    @Value("${spring.openapi.assembly.url}")
//    private String baseUrl;
//
//    @Value("${spring.openapi.assembly.key}")
//    private String apikey;
//
//
//    /**
//     * 국회방송국 API에서 특정 인물의 뉴스/발언 정보를 가져와 저장
//     * @param figureName
//     * @return
//     */
//    @Transactional
//    public int syncStatementsByFigure(String figureName) {
//        log.info("국회방송국 API에서 '{}' 인물 발언 정보 동기화 시작", figureName);
//
//        Figure figure = figureRepository.findByName(figureName)
//                .orElseThrow(() -> new EntityNotFoundException("해당 인물이 존재하지 않습니다: " + figureName));
//
//        AssemblyApiResponse<String> apiResponse = fetchStatementsByFigure(figureName);
//        if (!apiResponse.isSuccess()) {
//            log.error("API 호출 실패: {}", apiResponse.resultMessage());
//            throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
//        }
//
//        List<StatementApiDTO> apiDtos = statementApiMapper.map(apiResponse);
//        int syncCount = 0;
//
//        for (StatementApiDTO dto : apiDtos) {
//            Optional<StatementDocument> existingStatement = statementRepository.findAll().stream()
//                    .filter(s -> s.getOriginalUrl().equals(dto.originalUrl()))
//                    .findFirst();
//
//            if (existingStatement.isPresent()) {
//                log.debug("이미 존재하는 발언 건너: {}", dto.originalUrl());
//                continue;
//            }
//
//            StatementDocument statement = convertToStatement(dto, figure);
//            statementRepository.save(statement);
//            syncCount++;
//        }
//
//        log.info("'{}' 인물 발언 정보 동기화 완료. 총 {} 건 처리됨", figureName, syncCount);
//        return syncCount;
//    }
//
//    /**
//     * API에서 특정 인물의 발언 정보 가져오기
//     * @param figureName
//     * @return
//     */
//    public AssemblyApiResponse<String> fetchStatementsByFigure(String figureName) {
//        WebClient webClient = webclientBuilder.baseUrl(baseUrl)
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
//                .build();
//
//        String xmlResponse = webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/news/figure")
//                        .queryParam("apiKey", apikey)
//                        .queryParam("name", figureName)
//                        .build())
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//        String resultCode = extractResultCode(xmlResponse);
//        String resultMessage = extractResultMessage(xmlResponse);
//
//        return new AssemblyApiResponse<>(resultCode, resultMessage, xmlResponse);
//    }
//
//    /**
//     * 특정 기간 동안의 모든 발언 정보를 동기화
//     * @param startDate
//     * @param endDate
//     * @return
//     */
//    @Transactional
//    public int syncStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
//        log.info("국회방송국 API에서 기간({} ~ {}) 발언 정보 동기화 시작", startDate, endDate);
//
//        AssemblyApiResponse<String> apiResponse = fetchStatementsByPeriod(startDate, endDate);
//        if (!apiResponse.isSuccess()) {
//            throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
//        }
//
//        List<StatementApiDTO> apiDtos = statementApiMapper.map(apiResponse);
//        int syncCount = 0;
//
//        for (StatementApiDTO dto : apiDtos) {
//            Optional<StatementDocument> existingStatement = statementRepository.findAll().stream()
//                    .filter(s -> s.getOriginalUrl().equals(dto.originalUrl()))
//                    .findFirst();
//
//            if (existingStatement.isPresent()) {
//                log.debug("이미 존재하는 발언 건너뜀: {}", dto.originalUrl());
//                continue;
//            }
//
//            Figure figure = figureRepository.findByName(dto.figureName())
//                    .orElseGet(() -> {
//                        Figure newFigure = Figure.builder()
//                                .name(dto.figureName())
//                                .build();
//                        return figureRepository.save(newFigure);
//                    });
//
//            StatementDocument statement = convert(dto, figure);
//            statementRepository.save(statement);
//            syncCount++;
//        }
//
//        log.info("기간({} ~ {}) 발언 정보 동기화 완료. 총 {} 건 처리됨", startDate, endDate, syncCount);
//        return syncCount;
//    }
//
//    /**
//     * API에서 특정 기간의 발언 정보 가져오기
//     * @param startDate
//     * @param endDate
//     * @return
//     */
//    public AssemblyApiResponse<String> fetchStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
//        WebClient webClient = webclientBuilder.baseUrl(baseUrl)
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
//                .build();
//
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//
//        String xmlResponse = webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/news/period")
//                        .queryParam("apiKey", apikey)
//                        .queryParam("startDate", startDate.format(formatter))
//                        .queryParam("endDate", endDate.format(formatter))
//                        .build())
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//        String resultCode = extractResultCode(xmlResponse);
//        String resultMessage = extractResultMessage(xmlResponse);
//
//        return new AssemblyApiResponse<>(resultCode, resultMessage, xmlResponse);
//    }
//
//    /**
//     * XML 응답에서 결과 메시지 추출
//     * @param xmlResponse
//     * @return
//     */
//    private String extractResultMessage(String xmlResponse) {
//        if (xmlResponse.contains("<MESSAGE>")) {
//            int start = xmlResponse.indexOf("<MESSAGE>") + "<MESSAGE>".length();
//            int end = xmlResponse.indexOf("</MESSAGE>");
//            if (start > 0 && end > start) {
//                return xmlResponse.substring(start, end);
//            }
//        }
//        return "처리 중 오류가 발생했습니다";
//    }
//
//    /**
//     * XML 응답에서 결과 코드 추출
//     * @param xmlResponse
//     * @return
//     */
//    private String extractResultCode(String xmlResponse) {
//        if (xmlResponse.contains("<CODE>")) {
//            int start = xmlResponse.indexOf("<CODE>") + "<CODE>".length();
//            int end = xmlResponse.indexOf("</CODE>");
//            if (start > 0 && end > start) {
//                return xmlResponse.substring(start, end);
//            }
//        }
//        return "99";
//    }
//}

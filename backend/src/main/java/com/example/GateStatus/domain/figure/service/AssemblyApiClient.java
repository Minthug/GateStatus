package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.GateStatus.domain.common.JsonUtils.isEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssemblyApiClient {

    private final WebClient assemblyWebClient;
    private final ObjectMapper objectMapper;
    private final FigureMapper figureMapper;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apiKey;

    @Value("${spring.openapi.assembly.figure-api-path}")
    private String figureApiPath;

    public FigureInfoDTO fetchFigureByName(String figureName) {
        log.info("국회의원 정보 API 호출: {}", figureName);

        try {
            String jsonResponse = assemblyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("HG_NM", figureName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (isEmpty(jsonResponse)) {
                log.warn("API 응답 없음: {}", figureName);
                return null;
            }

            List<FigureInfoDTO> figures = parseApiResponse(jsonResponse);
            return figures.isEmpty() ? null : figures.get(0);
        } catch (Exception e) {
            log.error("API 호출 실패: {} - {}", figureName, e.getMessage(), e);
            throw new ApiDataRetrievalException("API 호출 실패: " + e.getMessage());
        }
    }

    public List<FigureInfoDTO> fetchAllFigures() {
        log.info("전체 국회의원 정보 API 호출 시작");
        List<FigureInfoDTO> allFigures = new ArrayList<>();

        int maxPage = 4;
        for (int pageNo = 1; pageNo <= maxPage; pageNo++) {
            try {
                List<FigureInfoDTO> pageFigures = fetchFiguresPage(pageNo, 100);

                if (pageFigures.isEmpty()) {
                    log.info("페이지 {}에서 더 이상 데이터 없음", pageNo);
                    break;
                }

                allFigures.addAll(allFigures);
                log.info("페이지 {} 완료: {}명", pageNo, pageFigures.size());
                if (pageFigures.size() < 100) {
                    log.info("마지막 페이지 도달: {}", pageNo);
                    break;
                }
            } catch (Exception e) {
                log.error("페이지 {} 호출 실패: {}", pageNo, e.getMessage(), e);
                break;
            }
        }
        log.info("전체 국회의원 정보 API 호출 완료: 총 {}명", allFigures.size());
        return allFigures;
    }

    public List<FigureInfoDTO> fetchFiguresByParty(String partyName) {
        log.info("{}당 소속 국회의원 정보 API 호출", partyName);

        try {
            String jsonResponse = assemblyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("POLY_NM", partyName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            List<FigureInfoDTO> figures = parseApiResponse(jsonResponse);
            log.info("{}당 소속 국회의원 정보 API 호출 완료: {}명", partyName, figures.size());
            return figures;
        } catch (Exception e) {
            log.error("정당별 API 호출 실패: {} - {}", partyName, e.getMessage(), e);
            throw new ApiDataRetrievalException("정당별 API 호출 실패: " + e.getMessage());
        }
    }

    private List<FigureInfoDTO> fetchFiguresPage(int pageNo, int pageSize) {
        String jsonResponse = assemblyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                    .path   (figureApiPath)
                    .queryParam("KEY", apiKey)
                    .queryParam("Type", "json")
                    .queryParam("pIndex", pageNo)
                    .queryParam("pSize", pageSize)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseApiResponse(jsonResponse);

    }


    private List<FigureInfoDTO> parseApiResponse(String jsonResponse) {
        if (isEmpty(jsonResponse)) {
            return Collections.emptyList();
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode rowsNode = rootNode.path("nwvrqwxyaytdsfvhu")
                    .path(1)
                    .path("row");

            if (!rowsNode.isArray()) {
                log.warn("API 응답에서 row 배열을 찾을 수 없음");
                return Collections.emptyList();
            }
            return figureMapper.mapFromJsonNode(rowsNode);
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("JSON 파싱 실패: " + e.getMessage());
        }
    }
//
//    private FigureInfoDTO parseFigureFromJsonNode(JsonNode row) {
//        String figureId = getTextValue(row, "MONA_CD");
//        String name = getTextValue(row, "HG_NM");
//
//        if (isEmpty(figureId)) {
//            log.warn("유효하지 않은 figureId: {}", figureId);
//            return null;
//        }
//
//        if (isEmpty(name)) {
//            log.warn("유효하지 않은 name: {}", name);
//            return null;
//        }
//
//        // 기본 정보 추출
//        String englishName = getTextValue(row, "ENG_NM");
//        String birth = getTextValue(row, "BTH_DATE");
//        String partyNameStr = getTextValue(row, "POLY_NM");
//        String constituency = getTextValue(row, "ORIG_NM");
//        String committeeName = getTextValue(row, "CMIT_NM");
//        String committeePosition = getTextValue(row, "JOB_RES_NM");
//        String electedCount = getTextValue(row, "REELE_GBN_NM");
//        String electedDate = getTextValue(row, "UNITS");
//        String reelection = getTextValue(row, "REELE_GBN_NM");
//        String email = getTextValue(row, "E_MAIL");
//        String homepage = getTextValue(row, "HOMEPAGE");
//
//        FigureParty partyName = convertToFigureParty(partyNameStr);
//
//        List<String> education = parseEducation(row);
//        List<Career> careers = parseCareers(row);
//
//        return new FigureInfoDTO(
//                figureId, name, englishName, birth, partyName, constituency,
//                committeeName, committeePosition, electedCount, electedDate,
//                reelection, null,
//                education,
//                careers,
//                email, homepage, null, null);
//    }
}

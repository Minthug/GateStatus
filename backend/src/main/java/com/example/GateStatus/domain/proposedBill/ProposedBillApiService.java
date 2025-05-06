package com.example.GateStatus.domain.proposedBill;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillApiDTO;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillApiMapper;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillApiService {

    private final WebClient webClient;
    private final ProposedBillApiMapper apiMapper;
    private final ObjectMapper mapper;
    private final ProposedBillRepository billRepository;
    private final FigureRepository figureRepository;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;
    @Value("${spring.openapi.assembly.key}")
    private String apiKey;
    @Value("${spring.openapi.assembly.proposed-bill-path}")
    private String proposedBillPath;

    /**
     * 특정 국회의원이 대표 발의한 법안 정보 동기화
     * @param proposedName
     * @return
     */
    @Transactional
    public int syncBillByProposer(String proposedName) {
        try {
            log.info("{}의 발의 법안 동기화 시작", proposedName);

            Figure figure = figureRepository.findByName(proposedName)
                    .orElseThrow(() -> new EntityNotFoundException("해당 국회의원 정보가 없습니다" + proposedName));

            List<ProposedBillApiDTO> bills = fetchBillsByProposerFromApi(proposedName);

            int count = 0;
            for (ProposedBillApiDTO dto : bills) {
                try {
                    ProposedBill bill = billRepository.findByBillId(dto.billId())
                            .orElseGet(() -> ProposedBill.builder()
                                    .billId(dto.billId())
                                    .proposer(figure)
                                    .viewCount(0)
                                    .build());

                    updateBillFromDto(bill, dto);

                    billRepository.save(bill);
                    count++;
                } catch (Exception e) {
                    log.error("법안 동기화 중 오류 발생: {}", dto.billId());
                }
            }

            log.info("{}의 발의 법안 동기화 완료: {}건", proposedName, count);
            return count;
        } catch (Exception e) {
            throw new ApiDataRetrievalException("발의 법안 정보를 동기화 중 오류 발생");
        }
    }

    /**
     * API에서 특정 국회의원의 발의 법안 조회
     * @param proposedName
     * @return
     */
    private List<ProposedBillApiDTO> fetchBillsByProposerFromApi(String proposedName) {
        try {
            log.info("{}의 발의 법안 API 호출", proposedName);

            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(proposedBillPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("PROPOSER", proposedName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (isEmpty(jsonResponse)) {
                log.warn("API에서 빈 응답 또는 null 반환 (이름: {}) ", proposedName);
                return Collections.emptyList();
            }

            log.debug("API 응답 수신 일부: {}", jsonResponse.substring(0, Math.min(100, jsonResponse.length())));

            List<ProposedBillApiDTO> bills =
            log.info("{}의 발의 법안 조회 결과: {}건", proposedName, bills.size());

            return bills;
        } catch (Exception e) {
            throw new ApiDataRetrievalException("발의 법안 정보 가져오는 중 오류 발생");
        }
    }

    /**
     * DTO 데이터로 법안 엔티티 업데이트
     * @param bill
     * @param dto
     */
    private void updateBillFromDto(ProposedBill bill, ProposedBillApiDTO dto) {
        bill.update(
                dto.billNo(),
                dto.billName(),
                parseDate(dto.proposedDate()),
                dto.summary(),
                dto.billUrl(),
                parsebillStatus(dto.billStatus(), dto.processResult()),
                parseDate(dto.processDate()),
                dto.processResult(),
                dto.committee(),
                dto.coProposers()
        );
    }

    /**
     * 법안 상태 코드 변환
     * @param statusCode
     * @param processResult
     * @return
     */
    private BillStatus parsebillStatus(String statusCode, String processResult) {
        if (statusCode == null || statusCode.isEmpty()) {
            return BillStatus.PROPOSED;
        }

        return switch (statusCode) {
            case "1" -> BillStatus.PASSED;
            case "2" -> BillStatus.REJECTED;
            case "3" -> BillStatus.WITHDRAWN;
            case "4" -> BillStatus.EXPIRED;
            default -> {
                if (processResult != null && processResult.contains("위원회")) {
                    yield BillStatus.IN_COMMITTEE;
                } else if (processResult != null && processResult.contains("본회의")) {
                    yield BillStatus.IN_PLENARY;
                } else {
                    yield BillStatus.PROPOSED;
                }
            }
        };
    }

    /**
     * 문자열 날짜를 LocalDate로 변환
     * @param dateStr
     * @return
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("날짜 형식 변환 오류: {}", dateStr, e);
            return null;
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private List<FigureInfoDTO> parseJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode rowsNode = rootNode.path("nzmimeepazxkubdpn")
                    .path(1)
                    .path("row");

            if (!rowsNode.isArray()) {
                log.warn("JSON 응답에서 row 배열을 찾을 수 없습니다");
                return Collections.emptyList();
            }

            List<ProposedBillApiDTO> bills = new ArrayList<>();
            int parsedCount = 0;
            int skipCount = 0;

            for (JsonNode row : rowsNode) {
                try {
                    ProposedBillApiDTO dto = parseFigureFromJsonNode(row);
                    if (dto != null) {
                        bills.add(dto);
                        parsedCount++;
                    } else {
                        skipCount++;
                    }
                } catch (Exception e) {
                    log.warn("법안 파싱 중 오류 발생: {}", e.getMessage());
                    skipCount++;
                }
            }

            log.info("법안 정보 파싱 완료: 성공 {}, 실패 {}", parsedCount, skipCount);
            return bills;
        } catch (Exception e) {
            log.error("JSON 파싱 중 오류 발생: {}", e.getMessage());
            throw new ApiDataRetrievalException("JSON 파싱 실패 " + e.getMessage());
        }
    }
}

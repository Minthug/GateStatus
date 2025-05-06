package com.example.GateStatus.domain.proposedBill;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillApiDTO;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillApiMapper;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillApiService {

    private final WebClient webClient;
    private final ProposedBillApiMapper apiMapper;
    private final ProposedBillService billService;
    private final ObjectMapper mapper;
    private final ProposedBillRepository billRepository;
    private final FigureRepository figureRepository;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;
    @Value("${spring.openapi.assembly.key}")
    private String apiKey;
    @Value("${spring.openapi.assembly.proposed-bill-path}")
    private String proposedBillPath;

    @Transactional
    public int syncAllBills() {
        log.info("모든 국회의원의 발의 법안 정보 동기화 시작");

        List<Figure> allFigures = figureRepository.findByFigureType(FigureType.POLITICIAN);

        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return 0;
        }

        log.info("동기화 대상 국회의원: {}명", allFigures.size());

        int totalSuccess = 0;
        int totalFail = 0;

        for (Figure figure : allFigures) {
            try {
                String name = figure.getName();
                log.info("국회의원 {}의 발의 법안 동기화 시작", name);

                int success = syncBillByProposer(name);
                totalSuccess += success;

                log.info("국회의원 {}의 발의 법안 동기화 완료: {}건", name, success);
            } catch (Exception e) {
                totalFail++;
                log.error("국회의원 {}의 발의 법안 동기화 중 오류: {}", figure.getName(), e.getMessage(), e);
            }
        }


        log.info("모든 국회의원 발의 법안 동기화 완료: 총 {}명 중 성공 {}건, 실패 {}건",
                allFigures.size(), totalSuccess, totalFail);
        return totalSuccess;
    }

    /**
     * 특정 국회의원이 대표 발의한 법안 정보 동기화
     * @return
     */
    @Transactional
    public int syncBillByProposer(String proposerName) {
        try {
            log.info("{}의 발의 법안 동기화 시작", proposerName);

            List<ProposedBillApiDTO> bills = fetchBillsByProposerFromApi(proposerName);

            if (bills.isEmpty()) {
                log.warn("동기화할 발의 법안 정보가 없습니다: {}", proposerName);
                return 0;
            }

            log.info("동기화 대상 법안: {}건", bills.size());


            int successCount = 0;
            int failCount = 0;


            for (ProposedBillApiDTO bill : bills) {
                try {
                    boolean success = billService.saveBill(bill, proposerName);
                    if (success) {
                        successCount++;
                        log.debug("법안 저장 성공: {}, ID={}", bill.billName(), bill.billId());
                    } else {
                        failCount++;
                        log.warn("법안 저장 실패: {}", bill.billName());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("법안 저장 중 오류: {} - {}", bill.billName(), e.getMessage(), e);
                }
            }

            log.info("{}의 발의 법안 동기화 완료: 총 {}건 중 {}건 성공, {}건 실패",
                    proposerName, bills.size(), successCount, failCount);
            return successCount;
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

            List<ProposedBillApiDTO> bills = parseJsonResponse(jsonResponse);
            log.info("{}의 발의 법안 조회 결과: {}건", proposedName, bills.size());

            return bills;
        } catch (Exception e) {
            throw new ApiDataRetrievalException("발의 법안 정보 가져오는 중 오류 발생");
        }
    }

//    /**
//     * DTO 데이터로 법안 엔티티 업데이트
//     * @param bill
//     * @param dto
//     */
//    private void updateBillFromDto(ProposedBill bill, ProposedBillApiDTO dto) {
//        bill.update(
//                dto.billNo(),
//                dto.billName(),
//                parseDate(dto.proposedDate()),
//                dto.summary(),
//                dto.billUrl(),
//                parsebillStatus(dto.billStatus(), dto.processResult()),
//                parseDate(dto.processDate()),
//                dto.processResult(),
//                dto.committee(),
//                dto.coProposers()
//        );
//    }

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


    private List<ProposedBillApiDTO> parseJsonResponse(String jsonResponse) {
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
                    String billId = getTextValue(row, "BILL_ID");
                    String billNo = getTextValue(row, "BILL_NO");
                    String billName = getTextValue(row, "BILL_NAME");
                    String proposer = getTextValue(row, "PROPOSER");

                    if (isEmpty(billId) || isEmpty(billName)) {
                        log.warn("유효하지 않은 법안 정보: ID={}, 이름={}", billId, billName);
                        skipCount++;
                        continue;
                    }

                    List<String> coProposers = parseCoProposers(row);

                    ProposedBillApiDTO dto = new ProposedBillApiDTO(
                            billId,
                            billNo,
                            billName,
                            proposer,
                            getTextValue(row, "PROPOSE_DT"),
                            getTextValue(row, "SUMMARY"),
                            getTextValue(row, "LINK_URL"),
                            getTextValue(row, "PROC_RESULT_CD"),
                            getTextValue(row, "PROC_DT"),
                            getTextValue(row, "PROC_RESULT"),
                            getTextValue(row, "COMMITTEE_NAME"),
                            coProposers
                    );

                    bills.add(dto);
                    parsedCount++;

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

    private List<String> parseCoProposers(JsonNode row) {
        String coProposerText = getTextValue(row, "COPROPOSER");
        if (coProposerText == null || coProposerText.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(coProposerText.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // 유틸리티 메서드
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}

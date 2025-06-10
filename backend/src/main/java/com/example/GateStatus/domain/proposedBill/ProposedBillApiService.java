package com.example.GateStatus.domain.proposedBill;

import com.example.GateStatus.domain.common.BillUtils;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.proposedBill.service.response.ProposedBillApiDTO;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.GateStatus.domain.common.JsonUtils.getTextValue;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillApiService {

    private final WebClient assemblyWebClient;
    private final ObjectMapper mapper;
    private final ProposedBillRepository billRepository;
    private final FigureRepository figureRepository;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;
    @Value("${spring.openapi.assembly.key}")
    private String apiKey;
    @Value("${spring.openapi.assembly.proposed-bill-path}")
    private String proposedBillPath;

    private static final String CURRENT_ASSEMBLY_AGE = "22";
    private static final String API_PATH_BILLS = "nzmimeepazxkubdpn";

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
        validateProposerName(proposerName);

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
                    saveBill(bill, proposerName);
                        successCount++;
                        log.debug("법안 저장 성공: {}, ID={}", bill.billName(), bill.billId());
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

    @Transactional
    public ProposedBill updateFromApiData(String billId, ProposedBillApiDTO apiData) {
        validateBillId(billId);
        validateApiData(apiData);

        Optional<ProposedBill> existingBill = billRepository.findByBillId(billId);
        ProposedBill bill;

        if (existingBill.isPresent()) {
            bill = existingBill.get();
            updateBillFromApiData(bill, apiData);
            log.debug("기존 법안 업데이트: {}", apiData.billName());
        } else {
            bill = createBillFromApiData(apiData);
            log.debug("새 법안 생성: {}", apiData.billName());
        }
        return billRepository.save(bill);
    }

    private ProposedBill createBillFromApiData(ProposedBillApiDTO dto) {
        Figure proposer = findProposerByName(dto.proposer());

        return ProposedBill.builder()
                .billId(dto.billId())
                .billNo(dto.billNo())
                .billName(dto.billName())
                .proposer(proposer)
                .proposeDate(BillUtils.safeParseDateWithLogging(dto.proposedDate(), "발의일"))
                .summary(dto.summary())
                .billUrl(dto.linkUrl())
                .billStatus(BillUtils.determineBillStatus(dto.processResult()))
                .processDate(BillUtils.safeParseDateWithLogging(dto.processDate(), "처리일"))
                .processResult(dto.processResult())
                .processResultCode(dto.processResultCode())
                .committee(dto.committeeName())
                .coProposers(dto.coProposers())
                .viewCount(0)  // 새 법안은 조회수 0
                .build();
    }

    private void updateBillFromApiData(ProposedBill bill, ProposedBillApiDTO dto) {
        Figure proposer = findProposerByName(dto.proposer());

        bill.setBillNo(dto.billNo());
        bill.setBillName(dto.billName());
        bill.setProposer(proposer);
        bill.setSummary(dto.summary());
        bill.setBillUrl(dto.linkUrl());
        bill.setCommittee(dto.committeeName());
        bill.setCoProposers(dto.coProposers());

        BillStatus newStatus = BillUtils.determineBillStatus(dto.processResult());
        LocalDate newProcessDate = BillUtils.safeParseDateWithLogging(dto.processDate(), "처리일");

        if (bill.getBillStatus() != newStatus) {
            log.info("법안 상태 변경: {} - {} → {}",
                    bill.getBillName(), bill.getBillStatus(), newStatus);
        }

        bill.setBillStatus(newStatus);
        bill.setProcessDate(newProcessDate);
        bill.setProcessResult(dto.processResult());
        bill.setProcessResultCode(dto.processResultCode());

        LocalDate newProposeDate = BillUtils.safeParseDateWithLogging(dto.proposedDate(), "발의일");
        if (newProposeDate != null && !newProposeDate.equals(bill.getProposeDate())) {
            log.warn("법안 발의일 변경 감지: {} - {} → {}",
                    bill.getBillName(), bill.getProposeDate(), newProposeDate);
            bill.setProposeDate(newProposeDate);
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

            String jsonResponse = assemblyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(proposedBillPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("PROPOSER", proposedName)
                            .queryParam("AGE", CURRENT_ASSEMBLY_AGE)
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

    private List<ProposedBillApiDTO> parseJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode rowsNode = rootNode.path(API_PATH_BILLS).path(1)
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
                    ProposedBillApiDTO dto = createBillDtoFromJsonNode(row);
                    if (isValidBillDto(dto)) {
                        bills.add(dto);
                        parsedCount++;
                    } else {
                        skipCount++;
                        log.debug("유효하지 않은 법안 정보 스킵: ID={}, 이름={}",
                                dto.billId(), dto.billName());
                    }
                } catch (Exception e) {
                    skipCount++;
                    log.warn("법안 파싱 중 오류 발생: {}", e.getMessage());
                }
            }

            log.info("법안 정보 파싱 완료: 성공 {}, 실패 {}", parsedCount, skipCount);
            return bills;
        } catch (Exception e) {
            log.error("JSON 파싱 중 오류 발생: {}", e.getMessage());
            throw new ApiDataRetrievalException("JSON 파싱 실패 " + e.getMessage());
        }
    }

    private ProposedBillApiDTO createBillDtoFromJsonNode(JsonNode row) {
        return new ProposedBillApiDTO(
                getTextValue(row, "BILL_ID"),
                getTextValue(row, "BILL_NO"),
                getTextValue(row, "BILL_NAME"),
                getTextValue(row, "PROPOSER"),
                getTextValue(row, "PROPOSE_DT"),
                getTextValue(row, "SUMMARY"),
                getTextValue(row, "LINK_URL"),
                getTextValue(row, "PROC_RESULT_CD"),
                getTextValue(row, "PROC_DT"),
                getTextValue(row, "PROC_RESULT"),
                getTextValue(row, "COMMITTEE_NAME"),
                parseCoProposers(row)
        );
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBill(ProposedBillApiDTO bill, String proposerName) {
        try {

            log.info("법안 저장 시작: {}, 발의자: {}", bill.billName(), proposerName);

            Optional<ProposedBill> existingBill = billRepository.findByBillId(bill.billId());
            ProposedBill bills;

            if (existingBill.isPresent()) {
                bills = existingBill.get();
                updateBillFromApiData(bills, bill);
            } else {
                bills = createBillFromApiData(bill);
                log.debug("새 법안 생성: {}", bill.billName());
            }

            billRepository.save(bills);
            log.debug("법안 저장 완료: {}", bill.billName());
        } catch (Exception e) {
            log.error("법안 저장 중 오류: {} - {}", bill.billName(), e.getMessage(), e);
            throw e;
        }
    }


    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ========================================
    // Validation Methods
    // ========================================

    private void validateProposerName(String proposerName) {
        if (isEmpty(proposerName)) {
            throw new IllegalArgumentException("발의자 이름은 필수입니다");
        }
    }

    private void validateBillId(String billId) {
        if (isEmpty(billId)) {
            throw new IllegalArgumentException("법안 ID는 필수입니다");
        }
    }

    private void validateApiData(ProposedBillApiDTO apiData) {
        if (apiData == null) {
            throw new IllegalArgumentException("API 데이터는 필수 입니다");
        }
    }

    private Figure findProposerByName(String proposerName) {
        if (isEmpty(proposerName)) {
            return null;
        }

        return figureRepository.findByName(proposerName.trim()).orElse(null);
    }

    /**
     * BillDTO 유효성 검증
     */
    private boolean isValidBillDto(ProposedBillApiDTO dto) {
        return !isEmpty(dto.billId()) && !isEmpty(dto.billName());
    }
}

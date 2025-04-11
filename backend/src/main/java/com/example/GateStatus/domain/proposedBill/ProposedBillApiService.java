package com.example.GateStatus.domain.proposedBill;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillApiDTO;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillApiMapper;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillApiService {

    private final WebClient webClient;
    private final ProposedBillApiMapper apiMapper;
    private final ProposedBillRepository billRepository;
    private final FigureRepository figureRepository;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;
    @Value("${spring.openapi.assembly.key}")
    private String apiKey;
    @Value("${spring.openapi.assembly.proposed-bill-path}")
    private String proposedBillPath;

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

    private List<ProposedBillApiDTO> fetchBillsByProposerFromApi(String proposedName) {
        try {
            log.info("{}의 발의 법안 API 호출", proposedName);

            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(proposedBillPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("PROPOSER", proposedName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();

            List<ProposedBillApiDTO> bills = apiMapper.map(apiResponse);
            log.info("{}의 발의 법안 조회 결과: {}건", proposedName, bills.size());

            return bills;
        } catch (Exception e) {
            throw new ApiDataRetrievalException("발의 법안 정보 가져오는 중 오류 발생");
        }
    }

    private void updateBillFromDto(ProposedBill bill, ProposedBillApiDTO dto) {

    }
}

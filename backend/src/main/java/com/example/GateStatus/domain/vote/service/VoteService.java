package com.example.GateStatus.domain.vote.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.vote.Vote;
import com.example.GateStatus.domain.vote.VoteResultType;
import com.example.GateStatus.domain.vote.dto.BillDetailDTO;
import com.example.GateStatus.domain.vote.dto.BillVoteDTO;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import com.example.GateStatus.global.config.exception.ApiClientException;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.example.GateStatus.global.config.exception.ApiServerException;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.example.GateStatus.global.config.redis.RedisCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final WebClient assemblyWebClient;
    private final FigureRepository figureRepository;
    private final VoteRepository voteRepository;
    private final ProposedBillRepository billRepository;
    private final RedisCacheService cacheService;
    private final BillApiMapper mapper;

    @Value("${spring.openapi.assembly.url}")
    private String voteApiUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apiKey;

    private static final int CACHE_TTL_SECONDS = 86400; // 1일
    private static final String ASSEMBLY_AGE = "21";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Transactional
    public List<BillVoteDTO> getVotesByFigureId(Long figureId) {
        String cacheKey = "votes:figure:" + figureId;
        List<BillVoteDTO> votes = cacheService.getOrSet(cacheKey, () -> fetchVotesFromApi(figureId), CACHE_TTL_SECONDS);

        saveVoteToDB(figureId, votes);

        return votes;
    }

    private void saveVoteToDB(Long figureId, List<BillVoteDTO> votes) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("정치인을 찾을 수 없습니다"));

        for (BillVoteDTO voteDTO : votes) {
            if (voteRepository.existsByFigureIdAndBillBillNo(figureId, voteDTO.billNo())) {
                continue;
            }

            ProposedBill bill = findOrCreateBill(voteDTO.billNo(), voteDTO.billName());

            LocalDate voteDate = null;
            if (voteDTO.voteDate() != null && !voteDTO.voteDate().isEmpty()) {
                try {
                    voteDate = LocalDate.parse(voteDTO.voteDate(), DATE_FORMATTER);
                } catch (Exception e) {
                    log.warn("날짜 파싱 오류: {}", voteDTO.voteDate());
                    voteDate = LocalDate.now();
                }
            }

            VoteResultType voteResult = voteDTO.voteResultType();

            Vote vote = Vote.builder()
                    .figure(figure)
                    .bill(bill)
                    .voteDate(voteDate)
                    .voteResult(voteResult)
                    .meetingName(voteDTO.committee())
                    .voteTitle(voteDTO.billName())
                    .build();

            voteRepository.save(vote);
        }
    }

    private ProposedBill findOrCreateBill(String billNo, String billName) {
        return billRepository.findByBillNo(billNo)
                .orElseGet(() -> {
                    ProposedBill newBill = ProposedBill.builder()
                            .billNo(billNo)
                            .billName(billName)
                            .build();
                    return billRepository.save(newBill);
                });
    }

    private List<BillVoteDTO> fetchVotesFromApi(Long figureId) {
        try {
            Figure figure = figureRepository.findById(figureId)
                    .orElseThrow(() -> new EntityNotFoundException("정치인을 찾을 수 없습니다"));

            log.info("국회 API 호출 시작: 정치인= {}", figure.getName());

            AssemblyApiResponse<JsonNode> apiResponse = assemblyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ncocpgfiaoituanbr")
                            .queryParam("KEY", apiKey)
                            .queryParam("AGE", ASSEMBLY_AGE)
                            .queryParam("PROPOSER", figure.getName())
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            Mono.error(new ApiClientException("API 클라이언트 오류: " + response.statusCode())))
                    .onStatus(status -> status.is5xxServerError(), response ->
                            Mono.error(new ApiServerException("API 서버 오류: " + response.statusCode())))
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();

            if (apiResponse == null) {
                log.warn("API 응답이 null 입니다. 빈 결과 반환");
                return Collections.emptyList();
            }

            List<BillVoteDTO> votes = mapper.mapToBillVoteDTOs(apiResponse);
            log.info("API 호출 완료: 정치인= {}, 결과 수= {}", figure.getName(), votes.size());
            return votes;

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("투표 데이터 조회 중 오류 발생", e);
            throw new ApiDataRetrievalException("투표 데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public BillDetailDTO getBillDetail(String billNo) {
        String cacheKey = "bill:detail:" + billNo;

        return cacheService.getOrSet(cacheKey, () -> fetchBillDetailFromApi(billNo), 86400);
    }

    private BillDetailDTO fetchBillDetailFromApi(String billNo) {
        try {
            log.info("법안 상세 정보 API 호출 시작: 법안번호 = {}", billNo);

            AssemblyApiResponse<JsonNode> apiResponse = assemblyWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ncocndinfopromain")
                            .queryParam("KEY", apiKey)
                            .queryParam("BILL_ID", billNo)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            Mono.error(new ApiClientException("API 클라이언트 오류: " + response.statusCode())))
                    .onStatus(status -> status.is5xxServerError(), response ->
                            Mono.error(new ApiServerException("API 서버 오류: " + response.statusCode())))
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
                    .block();

            BillDetailDTO result = mapper.mapToBillDetail(apiResponse, billNo);

            log.info("법안 상세 정보 API 호출 완료: {}", billNo);
            return result;

        } catch (ApiClientException | ApiServerException e) {
            log.error("법안 상세 정보 API 호출 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("법안 상세 정보 조회 중 예외 발생", e);
            throw new ApiDataRetrievalException("법안 상세 정보를 가져오는 중 오류 발생: ");
        }
    }

    @Transactional(readOnly = true)
    public Page<FigureInfoDTO> searchPoliticiansByKeyword(String name, Pageable pageable) {
        Page<Figure> figures = figureRepository.findByNameContaining(name, pageable);
        return figures.map(FigureInfoDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<BillVoteDTO> getVotesByFigureName(String figureName, Pageable pageable) {
        Figure figure = figureRepository.findByName(figureName)
                .orElseThrow(() -> new EntityNotFoundException("정치인을 찾을 수 없습니다: " + figureName));

        Page<Vote> votePage = voteRepository.findByFigureId(figure.getId(), pageable);

        return votePage.map(vote -> {
            ProposedBill bill = vote.getBill();

            return new BillVoteDTO(
                    bill.getBillNo(),                               // 의안번호
                    vote.getVoteTitle() != null ? vote.getVoteTitle() : bill.getBillName(),  // 의안명
                    figure.getName(),                              // 제안자 - 현재 정치인 이름으로 설정
                    vote.getMeetingName(),                         // 소관위원회
                    bill.getProposeDate() != null ? bill.getProposeDate().toString() : "",  // 제안일자
                    vote.getVoteResult().getDisplayName(),         // 표결 결과
                    vote.getVoteDate() != null ? vote.getVoteDate().toString() : "",  // 표결일자
                    vote.getVoteResult(),                          // 표결 결과 타입
                    bill.getBillStatus() != null ? bill.getBillStatus().toString() : "",  // 법안 상태
                    bill.getBillUrl()                              // 법안 URL
            );
        });
    }
}


package com.example.GateStatus.domain.vote.service;

import com.example.GateStatus.domain.vote.VoteResultType;
import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BillApiMapper implements ApiMapper<JsonNode, List<BillVoteDTO>> {

    private final ObjectMapper objectMapper;

    @Override
    public List<BillVoteDTO> map(AssemblyApiResponse<JsonNode> response) {
        return mapToBillVoteDTOs(response);
    }

    /**
     * 국회 OPEN API 응답을 BillVoteDTO 리스트로 변환
     */
    public List<BillVoteDTO> mapToBillVoteDTOs(AssemblyApiResponse<JsonNode> apiResponse) {
        if (apiResponse == null || apiResponse.data() == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode dataArray = apiResponse.data();
            List<BillVoteDTO> result = new ArrayList<>();

            // JSON 배열을 순회하며 DTO로 변환
            if (dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    String voteResult = getTextValue(item, "RESULT_VOTE_MOD_NM");
                    // Record는 생성자를 통해 직접 초기화
                    BillVoteDTO dto = new BillVoteDTO(
                            getTextValue(item, "BILL_NO"),
                            getTextValue(item, "BILL_NM"),
                            getTextValue(item, "PROPOSER"),
                            getTextValue(item, "COMMITTEE_NM"),
                            getTextValue(item, "PROPOSE_DT"),
                            voteResult,
                            getTextValue(item, "VOTE_DT"),
                            VoteResultType.fromString(voteResult),
                            getTextValue(item, "PROC_RESULT"),
                            getTextValue(item, "LINK_URL")
                    );

                    result.add(dto);
                }
            }

            return result;
        } catch (Exception e) {
            throw new ApiMappingException("API 응답 매핑 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 법안 상세 정보를 DTO로 변환
     */
    public BillDetailDTO mapToBillDetail(AssemblyApiResponse<JsonNode> apiResponse, String billNo) {
        if (apiResponse == null || apiResponse.data() == null) {
            return BillDetailDTO.empty(billNo);
        }

        try {
            JsonNode dataNode = apiResponse.data();

            if (!dataNode.isArray() || dataNode.size() == 0) {
                return BillDetailDTO.empty(billNo);
            }

            JsonNode billInfo = dataNode.get(0);

            return new BillDetailDTO(
                    getTextValue(billInfo, "BILL_ID"),
                    getTextValue(billInfo, "BILL_NO"),
                    getTextValue(billInfo, "BILL_NAME"),
                    getTextValue(billInfo, "COMMITTEE"),
                    getTextValue(billInfo, "PROPOSER"),
                    getTextValue(billInfo, "PROPOSE_DT"),
                    getTextValue(billInfo, "PROC_RESULT"),
                    getTextValue(billInfo, "PROC_DT"),
                    getTextValue(billInfo, "BILL_URL"),
                    getTextValue(billInfo, "DETAIL_CONTENT"),
                    getTextValue(billInfo, "PROPOSER_INFO"),
                    parseVoteResults(billInfo)
            );
        } catch (Exception e) {
            throw new ApiMappingException("API 응답 매핑 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 법안 투표 결과 파싱
     */
    private VoteResultDetail parseVoteResults(JsonNode billInfo) {
        try {
            int agreeCount = getIntValue(billInfo, "AGREE_COUNT");
            int disagreeCount = getIntValue(billInfo, "DISAGREE_COUNT");
            int abstainCount = getIntValue(billInfo, "ABSTAIN_COUNT");
            int absentCount = getIntValue(billInfo, "ABSENT_COUNT");

            return new VoteResultDetail(
                    agreeCount,
                    disagreeCount,
                    abstainCount,
                    absentCount,
                    agreeCount + disagreeCount + abstainCount + absentCount
            );
        } catch (Exception e) {
            return new VoteResultDetail(0, 0, 0, 0, 0);
        }
    }

    /**
     * JsonNode에서 정수 값 추출
     */
    private int getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return 0;
        }

        try {
            return field.asInt();
        } catch (Exception e) {
            // 숫자 형식이 아니면 0 반환
            return 0;
        }
    }

    /**
     * JsonNode에서 텍스트 값 추출
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }

    /**
     * 국회 OPEN API 응답을 다른 DTO 타입으로 변환(다른 API에 활용)
     */
    public <T> List<T> mapToList(AssemblyApiResponse<JsonNode> apiResponse, Class<T> targetClass) {
        if (apiResponse == null || apiResponse.data() == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode dataArray = apiResponse.data();
            List<T> result = new ArrayList<>();

            if (dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    T dto = objectMapper.treeToValue(item, targetClass);
                    result.add(dto);
                }
            }
            return result;
        } catch (Exception e) {
            throw new ApiMappingException("API 응답 매핑 중 오류 발생: " + e.getMessage());
        }
    }
}

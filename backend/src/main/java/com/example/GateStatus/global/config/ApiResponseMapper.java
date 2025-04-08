package com.example.GateStatus.global.config;

import com.example.GateStatus.domain.vote.service.BillVoteDTO;
import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.GateStatus.domain.vote.service.BillVoteDTO.getTextValue;

@Component
@RequiredArgsConstructor
public class ApiResponseMapper {

    private final ObjectMapper objectMapper;

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
                    // Record는 생성자를 통해 직접 초기화
                    BillVoteDTO dto = new BillVoteDTO(
                            getTextValue(item, "BILL_NO"),
                            getTextValue(item, "BILL_NM"),
                            getTextValue(item, "PROPOSER"),
                            getTextValue(item, "COMMITTEE_NM"),
                            getTextValue(item, "PROPOSE_DT"),
                            getTextValue(item, "RESULT_VOTE_MOD_NM"),
                            getTextValue(item, "VOTE_DT")
                    );

                    result.add(dto);
                }
            }

            return result;
        } catch (Exception e) {
            throw new ApiMappingException("API 응답 매핑 중 오류 발생: " + e.getMessage());
        }
    }

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

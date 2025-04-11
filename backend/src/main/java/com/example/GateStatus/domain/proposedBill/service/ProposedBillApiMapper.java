package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProposedBillApiMapper implements ApiMapper<JsonNode, List<ProposedBillApiDTO>> {

    @Override
    public List<ProposedBillApiDTO> map(AssemblyApiResponse<JsonNode> response) {
        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode dataArray = response.data();
            List<ProposedBillApiDTO> result = new ArrayList<>();

            if (dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    List<String> coProposers = parseCoProposers(item);

                    ProposedBillApiDTO dto = new ProposedBillApiDTO(
                            getTextValue(item, "BILL_ID"),
                            getTextValue(item, "BILL_NO"),
                            getTextValue(item, "BILL_NAME"),
                            getTextValue(item, "PROPOSER"),
                            getTextValue(item, "PROPOSE_DT"),
                            getTextValue(item, "SUMMARY"),
                            getTextValue(item, "LINK_URL"),
                            getTextValue(item, "PROC_RESULT_CD"),
                            getTextValue(item, "PROC_DT"),
                            getTextValue(item, "PROC_RESULT"),
                            getTextValue(item, "COMMITTEE_NAME"),
                            coProposers
                    );
                    result.add(dto);
                }
            }
            return result;
        } catch (Exception e) {
            throw new ApiMappingException("법안 정보 매핑 중 오류 발생: ");
        }
    }

    private List<String> parseCoProposers(JsonNode item) {
        String coProposersText = getTextValue(item, "COPROPOSER");
        if (coProposersText == null || coProposersText.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(coProposersText.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }


    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }
}

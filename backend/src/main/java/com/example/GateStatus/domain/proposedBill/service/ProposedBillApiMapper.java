package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProposedBillApiMapper implements ApiMapper<JsonNode, List<ProposedBillApiDTO>> {

    @Override
    public List<ProposedBillApiDTO> map(AssemblyApiResponse<JsonNode> response) {
        return List.of();
    }
}

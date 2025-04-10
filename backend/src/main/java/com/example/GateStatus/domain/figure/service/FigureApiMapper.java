package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FigureApiMapper implements ApiMapper<JsonNode, List<FindFigureDetailResponse>> {

    private final ObjectMapper objectMapper;
}

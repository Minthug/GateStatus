package com.example.GateStatus.domain.dashboard.dto.response;

import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;

import java.util.List;

public record DashBoardResponse(
        FigureInfoDTO figure,
        BillStatistics billStatistics,
        StatementStatistics statementStatistics,
        VoteStatistics voteStatistics,
        List<KeywordDTO> keywords,
        List<BillOverTimeDTO> billOverTime) {
}

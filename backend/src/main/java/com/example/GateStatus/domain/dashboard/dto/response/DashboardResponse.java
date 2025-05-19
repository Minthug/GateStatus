package com.example.GateStatus.domain.dashboard.dto.response;

import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;

import java.util.List;

public record DashboardResponse(
        FigureInfoDTO figure,
        BillStatistics billStatistics,
        StatementStatistics statementStatistics,
        VoteStatistics voteStatistics,
        List<KeywordDTO> keywords,
        List<BillOverTimeDTO> billsOverTime) {

    public static DashboardResponse empty() {
        return new DashboardResponse(null, null, null, null, List.of(), List.of());
    }

    // 특정 필드만 변경한 새 인스턴스를 반환하는 with 메서드
    public DashboardResponse withFigure(FigureInfoDTO figure) {
        return new DashboardResponse(figure, this.billStatistics, this.statementStatistics,
                this.voteStatistics, this.keywords, this.billsOverTime);
    }
}

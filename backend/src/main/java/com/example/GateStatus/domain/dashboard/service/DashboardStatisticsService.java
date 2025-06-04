package com.example.GateStatus.domain.dashboard.service;

import com.example.GateStatus.domain.dashboard.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public class DashboardStatisticsService {
    public List<KeywordDTO> getKeywords(Long figureId, LocalDate startDate, LocalDate endDate) {
    }

    public List<BillOverTimeDTO> getBillOverTime(Long figureId, LocalDate startDate, LocalDate endDate) {
    }

    public VoteStatistics getVoteStatistics(Long figureId, LocalDate startDate, LocalDate endDate) {
    }

    public StatementStatistics getStatementStatistics(Long figureId, LocalDate startDate, LocalDate endDate) {
    }

    public BillStatistics getBillStatistics(Long figureId, LocalDate startDate, LocalDate endDate) {
    }
}

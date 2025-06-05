package com.example.GateStatus.domain.dashboard.dto.response;

import com.example.GateStatus.domain.vote.Vote;

import java.util.Map;

public record ComparisonStatistics(
        BillStatistics billStatistics,
        StatementStatistics statementStatistics,
        VoteStatistics voteStatistics
) {

    /**
     * 데이터 없을 때 기본 값으로 사용
     * @return
     */
    public static ComparisonStatistics empty() {
        return new ComparisonStatistics(
                new BillStatistics(0, 0, 0, 0, 0, 0, 0.0),
                new StatementStatistics(0, Map.of(), "없음"),
                new VoteStatistics(0, 0, 0, 0, 0.0, 0.0)
                );
    }

    public static ComparisonStatistics withBillStats(BillStatistics billStats) {
        return new ComparisonStatistics(
                billStats,
                new StatementStatistics(0, Map.of(), "없음"),
                new VoteStatistics(0,0,0, 0, 0.0,0.0));
    }

    public static ComparisonStatistics withStateStats(StatementStatistics stateStats) {
        return new ComparisonStatistics(
                new BillStatistics(0, 0, 0, 0,0,0, 0.0),
                stateStats,
                new VoteStatistics(0, 0, 0, 0, 0.0, 0.0)
        );
    }

    public static ComparisonStatistics withVoteStats(VoteStatistics voteStats) {
        return new ComparisonStatistics(
                new BillStatistics(0, 0, 0, 0, 0, 0, 0.0),
                new StatementStatistics(0, Map.of(), "없음"),
                voteStats);
    }



}

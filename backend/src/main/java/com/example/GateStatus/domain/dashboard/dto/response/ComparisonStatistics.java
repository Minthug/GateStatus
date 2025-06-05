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

    public double calculateActivityScore() {
        double billScore = Math.min(billStatistics.total() * 2, 50); // 법안 발의 (최대 50점)
        double statementScore = Math.min(statementStatistics.total() * 0.1, 30); // 발언 (최대 30점)
        double voteScore = voteStatistics.participationRate() * 0.2; // 투표 참여율 (최대 20점)

        return Math.min(billScore + statementScore + voteScore, 100); // 활동 점수 (0점-100점)
    }

    public double calculateEfficiencyScore() {
        double passRateScore = billStatistics.passRate() * 0.6; // 법안 통과율 (최대 60점)
        double consistencyScore = calculateVoteConsistency() * 0.4; // 투표 일관성 (최대 40점)
        
        return Math.min(passRateScore + consistencyScore, 100); // 효율성 점수 (0점 - 100점)
    }

    private double calculateVoteConsistency() {
        int totalVotes = voteStatistics.agree() + voteStatistics.disagree();
        if (totalVotes == 0) return 0;

        double agreeRatio = (double) voteStatistics.agree() / totalVotes;
        double disagreeRatio = (double) voteStatistics.disagree() / totalVotes;

        return Math.max(agreeRatio, disagreeRatio) * 100; // 한쪽으로 치우칠수록 일관성이 높다 판단
    }

    // 모든 통계가 0이면 true
    public boolean isEmpty() {
        return billStatistics.total() == 0 && statementStatistics.total() == 0 &&
                (voteStatistics.agree() + voteStatistics.disagree() + voteStatistics.abstain() + voteStatistics.absent()) == 0;
    }


}

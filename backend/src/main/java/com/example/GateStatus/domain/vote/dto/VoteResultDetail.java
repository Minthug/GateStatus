package com.example.GateStatus.domain.vote.dto;

public record VoteResultDetail(int agreeCount,
                               int disagreeCount,
                               int abstainCount,
                               int absentCount,
                               int totalCount) {

    /**
     * 찬성률 계산(%)
     * @return
     */
    public double getAgreeRate() {
        if (totalCount == 0) return 0.0;
        return Math.round((agreeCount * 100.0) / totalCount * 10) / 10.0;
    }

    /**
     * 반대율 계산(%)
     * @return
     */
    public double getDisagreeRate() {
        if (totalCount == 0) return 0.0;
        return Math.round((disagreeCount * 100.0) / totalCount * 10) / 10.0;
    }

    /**
     * 기권율 계산(%)
     * @return
     */
    public double getAbstainRate() {
        if (totalCount == 0) return 0.0;
        return Math.round((abstainCount * 100.0) / totalCount * 10) / 10.0;
    }
}

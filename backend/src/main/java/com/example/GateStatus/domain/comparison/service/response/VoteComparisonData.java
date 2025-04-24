package com.example.GateStatus.domain.comparison.service.response;

import java.util.List;

public record VoteComparisonData(
        List<VoteInfo> votes, // 관련 투표 목록
        int agreeCount, // 찬성 수
        int disagreeCount, // 반대 수
        int abstainCount, // 기권 수
        double agreementRate // 찬성률
) {
}

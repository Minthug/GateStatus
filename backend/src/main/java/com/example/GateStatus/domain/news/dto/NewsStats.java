package com.example.GateStatus.domain.news.dto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/*
 * 뉴스 통계 정보
 */
public record NewsStats(
        String keyword,
        long count,
        LocalDateTime from,
        LocalDateTime to
) {

    public double dailyAverage() {
        long days = ChronoUnit.DAYS.between(from, to);
        return days > 0 ? (double) count / days : count;
    }
}

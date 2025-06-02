package com.example.GateStatus.domain.common;

import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Value
@Builder
@With
public class DateRange {

    @NonNull
    private final LocalDate startDate;

    @NonNull
    private final LocalDate endDate;

    public static DateRange defaultRange() {
        return DateRange.builder()
                .startDate(LocalDate.now().minusYears(1))
                .endDate(LocalDate.now())
                .build();
    }

    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return DateRange.builder()
                .startDate(startDate != null ? startDate : LocalDate.now().minusYears(1))
                .endDate(endDate != null ? endDate : LocalDate.now())
                .build();
    }

    public static DateRange fromRequest(ComparisonRequest request) {
        if (request == null) {
            return defaultRange();
        }
        return of(request.startDate(), request.endDate());
    }

    public static DateRange ofPeriod(long amount, ChronoUnit unit) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minus(amount, unit);
        return of(startDate, endDate);
    }

    public static DateRange ofLastMonths(long months) {
        return ofPeriod(months, ChronoUnit.MONTHS);
    }

    public static DateRange ofLastYears(long years) {
        return ofPeriod(years, ChronoUnit.YEARS);
    }

    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public long getMonths() {
        return ChronoUnit.MONTHS.between(startDate, endDate);
    }

    public long getYears() {
        return ChronoUnit.YEARS.between(startDate, endDate);
    }

    public boolean isValid() {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }

    public boolean contains(LocalDate date) {
        if (date == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean overlaps(DateRange other) {
        if (other == null) return false;
        return !endDate.isBefore(other.startDate) && !startDate.isAfter(other.endDate);
    }

    public List<DateRange> splitByMonths() {
        List<DateRange> ranges = new ArrayList<>();
        LocalDate current = startDate.withDayOfMonth(1);

        while (!current.isAfter(endDate)) {
            LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
            LocalDate rangeEnd = monthEnd.isAfter(endDate) ? endDate : monthEnd;

            if (!current.isAfter(endDate)) {
                ranges.add(DateRange.of(current, rangeEnd));
            }

            current = current.plusMonths(1).withDayOfMonth(1);
        }
        return ranges;
    }

    public List<DateRange> splitByQuarters() {
        List<DateRange> ranges = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDate quarterEnd = getQuarterEnd(current);
            LocalDate rangeEnd = quarterEnd.isAfter(endDate) ? endDate : quarterEnd;

            ranges.add(DateRange.of(current, rangeEnd));
            current = quarterEnd.plusDays(1);
        }

        return ranges;
    }

    public double calculateActivityDensity(int activeDays) {
        long totalDays = getDays();
        if (totalDays == 0) return 0.0;
        return Math.min(1.0, (double) activeDays / totalDays);
    }

    public double calculateMonthlyAverage(int totalActivity) {
        long months = Math.max(1, getMonths());
        return (double) totalActivity / months;
    }

    public DateRange extend(long amount, ChronoUnit unit) {
        return DateRange.of(
                startDate.minus(amount, unit),
                endDate.plus(amount, unit)
        );
    }

    public String getDescription() {
        return String.format("%s ~ %s (%d일)", startDate, endDate, getDays());
    }

    public String getShortDescription() {
        if (getYears() >= 1) {
            return String.format("%d년 %d월 ~ %d년 %d월",
                    startDate.getYear(), startDate.getMonthValue(),
                    endDate.getYear(), endDate.getMonthValue());
        } else {
            return String.format("%d월 ~ %d월 (%d일)",
                    startDate.getMonthValue(), endDate.getMonthValue(), getDays());
        }
    }

    /**
     * 디버깅 및 로깅용 상세 정보
     */
    @Override
    public String toString() {
        return String.format("DateRange{start=%s, end=%s, days=%d, valid=%s}",
                startDate, endDate, getDays(), isValid());
    }

    // === Private Helper Methods ===
    private LocalDate getQuarterEnd(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        int endMonth = quarter * 3;
        return LocalDate.of(date.getYear(), endMonth, 1)
                .withDayOfMonth(LocalDate.of(date.getYear(), endMonth, 1).lengthOfMonth());
    }

    // === Validation Methods ===

    /**
     * 생성자에서 호출되는 유효성 검증
     */
    @Builder
    private DateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("시작 날짜는 null일 수 없습니다");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("종료 날짜는 null일 수 없습니다");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    String.format("시작 날짜(%s)는 종료 날짜(%s)보다 이전이어야 합니다", startDate, endDate));
        }

        this.startDate = startDate;
        this.endDate = endDate;
    }

}


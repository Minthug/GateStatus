package com.example.GateStatus.domain.common;

import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
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


    // === Private Helper Methods ===
    private LocalDate getQuarterEnd(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        int endMonth = quarter * 3;
        return LocalDate.of(date.getYear(), endMonth, 1)
                .withDayOfMonth(LocalDate.of(date.getYear(), endMonth, 1).lengthOfMonth());
    }



}


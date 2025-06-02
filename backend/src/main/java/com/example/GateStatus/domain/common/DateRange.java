package com.example.GateStatus.domain.common;

import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

import java.time.LocalDate;

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

    public
}

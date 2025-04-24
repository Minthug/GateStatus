package com.example.GateStatus.domain.comparison.service.response;

import java.time.LocalDate;

public record BillInfo(
        Long id,
        String billName,
        LocalDate proposeDate,
        String billStatus,
        boolean isPassed
) {
}

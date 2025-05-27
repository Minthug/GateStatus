package com.example.GateStatus.domain.news.dto;

import java.time.LocalDateTime;

public record CollectResponse(
        int collectedCount,
        String message,
        LocalDateTime timestamp
) {
}

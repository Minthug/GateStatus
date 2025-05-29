package com.example.GateStatus.domain.issue.service.response;

import java.time.LocalDateTime;

public record LinkResponse(
        String message,
        String issueId,
        String resourceType,
        String resourceId,
        LocalDateTime timestamp
) {
}

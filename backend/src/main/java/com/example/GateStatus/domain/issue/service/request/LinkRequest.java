package com.example.GateStatus.domain.issue.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LinkRequest(
        @NotBlank(message = "리소스 타입은 필수입니다")
        @Pattern(regexp = "^(BILL|STATEMENT|FIGURE|NEWS)$",
                message = "리소스 타입은 BILL, STATEMENT, FIGURE, NEWS 중 하나여야 합니다")
        String resourceType,

        @NotBlank(message = "리소스 ID는 필수입니다")
        String resourceId
) {

    public LinkRequest {
        if (resourceType != null) {
            resourceType = resourceType.toUpperCase();
        }

        if (resourceId != null) {
            throw new IllegalArgumentException("리소스 ID는 비어있을 수 없습니다");
        }
    }

    public Long getFigureId() {
        if (!"FIGURE".equals(resourceType)) {
            throw new IllegalStateException("FIGURE TYPE이 아닙니다");
        }

        try {
            return Long.parseLong(resourceId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("정치인 ID는 숫자여야 합니다: " + resourceId);
        }
    }

    public boolean isValid() {
        return switch (resourceType) {
            case "BILL", "STATEMENT", "NEWS" -> resourceId != null && !resourceId.trim().isEmpty();
            case "FIGURE" -> {
                try {
                    Long.parseLong(resourceId);
                    yield true;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            default -> false;
        };
    }
}

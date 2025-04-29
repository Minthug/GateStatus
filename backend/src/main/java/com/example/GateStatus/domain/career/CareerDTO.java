package com.example.GateStatus.domain.career;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CareerDTO {
    private String period; // 기간
    private String position; // 직위
    private String organization; // 소속
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;

    public static CareerDTO from(Career career) {
        return new CareerDTO(
                career.getPeriod(),
                career.getPosition(),
                career.getOrganization(),
                career.getTitle(),
                career.getStartDate(),
                career.getEndDate()
        );
    }
}


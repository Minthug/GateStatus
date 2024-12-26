package com.example.GateStatus.domain.career;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class Career {
    private String period; // 기간
    private String position; // 직위
    private String organization; // 소속

    private LocalDate startDate;
    private LocalDate endDate;
}

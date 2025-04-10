package com.example.GateStatus.domain.career;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Career {
    private String period; // 기간
    private String position; // 직위
    private String organization; // 소속
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;

    public static Career of(String title, String position, String organization, LocalDate startDate, LocalDate endDate) {
        return Career.builder()
                .title(title)
                .position(position)
                .organization(organization)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    public static String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) return "";

        String start = startDate.format(DateTimeFormatter.ofPattern("yyyy.MM"));
        String end = endDate == null ? "현재" : endDate.format(DateTimeFormatter.ofPattern("yyyy.MM"));

        return start + " ~ " + end;
    }
}

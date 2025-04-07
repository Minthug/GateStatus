package com.example.GateStatus.domain.career;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrofit2.http.GET;

import java.time.LocalDate;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Education {
    private String school;
    private String major;       // 전공
    private String degree;      // 학위
    private LocalDate startDate; // 입학일
    private LocalDate endDate;   // 졸업일
    private String description; // 추가 설명

}

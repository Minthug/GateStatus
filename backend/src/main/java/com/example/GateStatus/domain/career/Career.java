package com.example.GateStatus.domain.career;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@Getter
public class Career implements Serializable {
    private String period; // 기간
    private String position; // 직위
    private String organization; // 소속
    private String title;

    @Builder
    public Career(String title, String position, String organization, String period) {
        this.title = title != null ? title : "";
        this.position = position != null ? position : "";
        this.organization = organization != null ? organization : "";
        this.period = period != null ? period : "";
    }

    // toString, equals, hashCode 메서드 추가
    @Override
    public String toString() {
        return "Career{" +
                "title='" + title + '\'' +
                ", position='" + position + '\'' +
                ", organization='" + organization + '\'' +
                ", period='" + period + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Career career = (Career) o;
        return Objects.equals(title, career.title) &&
                Objects.equals(position, career.position) &&
                Objects.equals(organization, career.organization) &&
                Objects.equals(period, career.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, position, organization, period);
    }
}

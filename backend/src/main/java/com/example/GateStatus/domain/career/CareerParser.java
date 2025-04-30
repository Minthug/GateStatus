package com.example.GateStatus.domain.career;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CareerParser {

    public List<Career> parseCareers(String careersText) {
        List<Career> careers = new ArrayList<>();

        if (careersText == null || careersText.isEmpty()) {
            return careers;
        }

        String[] lines = careersText.split("\r\n");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            try {
                Career career = parseCareerLine(line);
                if (career != null) {
                    careers.add(career);
                }
            } catch (Exception e) {
                log.warn("경력 정보 파싱 중 오류: {}, 오류: {}", line, e.getMessage());
            }
        }

        return careers;
    }

    private Career parseCareerLine(String line) {
        String[] parts = line.trim().split(" / ");

        if (parts.length < 2) {
            log.warn("경력 정보 파싱 실패: {}", line);
            return null;
        }

        // 기간 파싱
        String[] periodParts = parts[0].split(" ~ ");
        LocalDate startDate = parseDateOrNull(periodParts[0]);
        LocalDate endDate = periodParts.length > 1 && !"현재".equals(periodParts[1])
                ? parseDateOrNull(periodParts[1])
                : null;

        // 직위와 소속 결정
        String position = parts.length > 1 ? parts[1] : "";
        String organization = parts.length > 2 ? parts[2] : "";

        return Career.of(
                line,           // title
                position,       // position
                organization,   // organization
                startDate,      // startDate
                endDate         // endDate
        );
    }

    private LocalDate parseDateOrNull(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || "현재".equals(dateStr.trim())) {
            return null;
        }

        try {
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy.MM"),
                    DateTimeFormatter.ofPattern("yyyy-MM"),
                    DateTimeFormatter.ofPattern("yyyy/MM")
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr.trim() + "-01",
                            formatter.withResolverStyle(ResolverStyle.STRICT));
                } catch (DateTimeParseException e) {
                    // 현재 포맷터로 파싱 실패하면 다음 포맷터 시도
                }
            }

            log.warn("날짜 파싱 실패: {}", dateStr);
            return null;
        } catch (Exception e) {
            log.warn("날짜 파싱 중 오류: {}, 오류: {}", dateStr, e.getMessage());
            return null;
        }
    }
}

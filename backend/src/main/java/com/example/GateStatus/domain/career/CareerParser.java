package com.example.GateStatus.domain.career;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

        // 줄바꿈 문자 처리 (\r\n, \n, \r 모두 지원)
        String[] lines = careersText.split("\\R");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            try {
                Career career = parseCareerLine(line);
                if (career == null) {
                    career = createSimpleCareer(line);
                } else {
                    careers.add(career);
                }
            } catch (Exception e) {
                log.warn("경력 정보 파싱 중 오류: {}, 오류: {}", line, e.getMessage());
                careers.add(createSimpleCareer(line));
            }
        }

        return careers;
    }

    /**
     * 단순 Career 객체 생성 (파싱 실패 시 대체용)
     * @param line
     * @return
     */
    private Career createSimpleCareer(String line) {
        return Career.builder()
                .title(line.trim())
                .position("")
                .organization("")
                .period("")
                .build();
    }

    /**
     * 단일 경력 텍스트 줄 파싱
     * @param line
     * @return
     */
    private Career parseCareerLine(String line) {
        try {
            String[] parts;
            if (line.contains(" / ")) {
                parts = line.trim().split(" / ");
            } else if (line.contains(" - ")) {
                parts = line.trim().split(" - ");
            } else {
                return createSimpleCareer(line);
            }

            if (parts.length < 2) {
                return createSimpleCareer(line);
            }

            // 기간 파싱
            String period = parts[0].trim();
            LocalDate startDate = null;
            LocalDate endDate = null;

            // 다양한 기간 형식 지원
            if (period.contains("~")) {
                String[] periodParts = period.split("~");
                startDate = parseDateOrNull(periodParts[0].trim());
                if (periodParts.length > 1) {
                    String endDateStr = periodParts[1].trim();
                    endDate = "현재".equals(endDateStr) ? null : parseDateOrNull(endDateStr);
                }
            } else if (period.contains("-")) {
                String[] periodParts = period.split("-");
                startDate = parseDateOrNull(periodParts[0].trim());
                if (periodParts.length > 1) {
                    String endDateStr = periodParts[1].trim();
                    endDate = "현재".equals(endDateStr) ? null : parseDateOrNull(endDateStr);
                }
            } else {
                startDate = parseDateOrNull(period);
            }

            String position = parts.length > 1 ? parts[1].trim() : "";
            String organization = parts.length > 2 ? parts[2].trim() : "";

            String periodStr = formatPeriod(startDate, endDate);

            return Career.builder()
                    .title(line)
                    .position(position)
                    .organization(organization)
                    .startDate(startDate)
                    .endDate(endDate)
                    .period(periodStr)
                    .build();
        } catch (Exception e) {
            log.debug("경력 라인 파싱 실패 (정규 형식 아님): {}", line);
            return null;
        }
    }

    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) return "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM");
        String start = startDate.format(formatter);
        String end = endDate == null ? "현재" : endDate.format(formatter);

        return start + " ~ " + end;
    }

    /**
     * 날짜 문자열 파싱
     * @param dateStr
     * @return
     */
    private LocalDate parseDateOrNull(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || "현재".equals(dateStr.trim())) {
            return null;
        }

        String trimmed = dateStr.trim();

        try {
            String[] patterns = {
                    "yyyy.MM", "yyyy-MM", "yyyy/MM",
                    "yyyy.M", "yyyy-M", "yyyy/M",
                    "yyyy년 MM월", "yyyy년 M월",
                    "yyyy"  // 연도만 있는 경우
            };

            for (String pattern : patterns) {
                try {
                   if (pattern.equals("yyyy")) {
                       return LocalDate.of(Integer.parseInt(trimmed), 1, 1);
                   }

                   DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    YearMonth yearMonth = YearMonth.parse(trimmed, formatter);
                    return yearMonth.atDay(1);
                } catch (DateTimeParseException e) {
                    // 현재 포맷터로 파싱 실패하면 다음 포맷터 시도
                }
            }

            if (trimmed.matches("\\d{4}")) {
                return LocalDate.of(Integer.parseInt(trimmed), 1, 1);
            }

            log.debug("지원되지 않은 날짜 형식: {}", dateStr);
            return null;
        } catch (Exception e) {
            log.warn("날짜 파싱 중 오류: {}, 오류: {}", dateStr);
            return null;
        }
    }
}

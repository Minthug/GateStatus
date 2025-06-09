package com.example.GateStatus.domain.common;

import com.example.GateStatus.domain.proposedBill.BillStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class BillUtils {

    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        String cleanDateStr = dateStr.trim();

        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),    // 2024-07-01
                DateTimeFormatter.ofPattern("yyyyMMdd"),      // 20240701
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),    // 2024.07.01
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),    // 2024/07/01
                DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"), // 2024년 07월 01일
                DateTimeFormatter.ofPattern("yyyy-M-d"),      // 2024-7-1 (0패딩 없음)
                DateTimeFormatter.ofPattern("yyyy.M.d"),      // 2024.7.1
                DateTimeFormatter.ofPattern("yyyy/M/d")       // 2024/7/1
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate parsedDate = LocalDate.parse(cleanDateStr, formatter);
                log.debug("날짜 파싱 성공: {} -> {}", dateStr, parsedDate);
                return parsedDate;
            } catch (Exception e) {
            }
        }

        log.warn("날짜 변환 실패 - 지원하지 않는 형식: '{}'. 지원 형식: yyyy-MM-dd, yyyyMMdd, yyyy.MM.dd, yyyy/MM/dd", dateStr);
        return null;
    }

    public static String normalizeDateString(String dateStr) {
        if (dateStr == null) return "null";

        String cleaned = dateStr.trim();
        return String.format("원본: '%s', 정리후: '%s', 길이: '%d'", dateStr, cleaned, cleaned.length());
    }
    
    public static LocalDate safeParseDateWithLogging(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            log.debug("{} 필드가 비어있음", fieldName);
            return null;
        }

        try {
            LocalDate result = parseDate(dateStr);
            if (result == null) {
                log.warn("{} 필드 날짜 파싱 실패: {}", fieldName, normalizeDateString(dateStr));
            } else {
                log.debug("{} 필드 날짜 파싱 성공: {} -> {}", fieldName, dateStr, result);
            }
            return result;
        } catch (Exception e) {
            log.error("{} 필드 날짜 파싱 중 예외 발생: {}, 오류: {}", fieldName, normalizeDateString(dateStr), e.getMessage());
            return null;
        }
    }

    public static BillStatus determineBillStatus(String processResult) {
        if (processResult == null || processResult.isEmpty()) {
            return BillStatus.PROPOSED;
        }

        // 가결 관련
        if (processResult.contains("원안가결") || processResult.contains("수정가결")) {
            return BillStatus.PASSED;
        }

        // 부결/폐기 관련
        if (processResult.contains("폐기") || processResult.contains("부결")) {
            return BillStatus.REJECTED;
        }

        // 대안반영
        if (processResult.contains("대안반영")) {
            return BillStatus.ALTERNATIVE;
        }

        // 철회
        if (processResult.contains("철회")) {
            return BillStatus.WITHDRAWN;
        }

        // 위원회 심사
        if (processResult.contains("위원회")) {
            return BillStatus.IN_COMMITTEE;
        }

        // 본회의 상정
        if (processResult.contains("본회의")) {
            return BillStatus.IN_PLENARY;
        }

        // 기타 모든 경우
        return BillStatus.PROCESSING;
    }
}

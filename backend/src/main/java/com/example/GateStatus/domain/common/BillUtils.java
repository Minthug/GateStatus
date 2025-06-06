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

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception ex) {
                log.warn("날짜 변환 실패: {}", dateStr);
                return null;
            }
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

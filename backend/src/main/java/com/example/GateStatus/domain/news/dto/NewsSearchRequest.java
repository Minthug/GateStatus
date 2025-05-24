package com.example.GateStatus.domain.news.dto;

/**
 * 뉴스 검색 요청 파라미터
 */
public record NewsSearchRequest(
        String query,
        int display,
        int start,
        String sort
) {

    public NewsSearchRequest {
        if (display < 1 || display > 100) {
            display = 10;
        }

        if (start < 1 || start > 100) {
            start = 1;
        }

        if (sort == null || (!sort.equals("sim") && !sort.equals("date"))) {
            sort = "date";
        }
    }

    public static NewsSearchRequest of(String query) {
        return new NewsSearchRequest(query, 10, 1, "date");
    }

    public NewsSearchRequest nextPage() {
        return new NewsSearchRequest(query, display, start + display, sort);
    }
}

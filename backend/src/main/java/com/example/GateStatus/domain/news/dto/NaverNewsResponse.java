package com.example.GateStatus.domain.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

public record NaverNewsResponse(
        @JsonProperty("lastBuildDate") String lastBuildDate,
        @JsonProperty("total") int total,
        @JsonProperty("start") int start,
        @JsonProperty("display") int display,
        @JsonProperty("items") List<Item> items
) {
    /**
     * 네이버 뉴스 아이템 DTO
     * 개별 뉴스 항목을 나타내는 불변 객체
     */
    public static record Item(
            @JsonProperty("title") String title,
            @JsonProperty("originallink") String originallink,
            @JsonProperty("link") String link,
            @JsonProperty("description") String description,
            @JsonProperty("pubDate") String pubDate
    ) {
        /**
         * HTML 태그가 제거된 제목 반환
         * @return 클린한 제목
         */
        public String getCleanTitle() {
            return removeHtmlTags(title);
        }

        /**
         * HTML 태그가 제거된 설명 반환
         * @return 클린한 설명
         */
        public String getCleanDescription() {
            return removeHtmlTags(description);
        }

        /**
         * HTML 태그 제거 헬퍼 메서드
         */
        private static String removeHtmlTags(String html) {
            if (html == null) return "";

            return html.replaceAll("<[^>]*>", "")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&nbsp;", " ")
                    .trim();
        }
    }

    /**
     * 유효한 응답인지 확인
     * @return 아이템이 있으면 true
     */
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    /**
     * 다음 페이지가 있는지 확인
     * @return 더 많은 결과가 있으면 true
     */
    public boolean hasNextPage() {
        return start + display < total;
    }
}

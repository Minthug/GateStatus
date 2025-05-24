package com.example.GateStatus.domain.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.example.GateStatus.domain.common.HtmlUtils.removeHtmlTags;

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

package com.example.GateStatus.domain.common;

public final class HtmlUtils {

    public HtmlUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     *
     * @param html
     * @return
     */
    public static String removeHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        return html.replaceAll("<[^>]*>", "")
                .replaceAll("&quot;", "\"")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&apos;", "'")
                .replaceAll("&#39;", "'")
                .replaceAll("&#x27;", "'")
                .replaceAll("&#x2F;", "/")
                .replaceAll("&#60;", "<")
                .replaceAll("&#62;", ">")
                .trim();
    }

    /**
     * 문자열 길이 제한 (뉴스 설명 등에 사용)
     * @param text
     * @param maxLength
     * @return
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

}

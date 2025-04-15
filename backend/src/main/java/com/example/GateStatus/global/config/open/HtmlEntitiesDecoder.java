package com.example.GateStatus.global.config.open;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Component;

@Component
public class HtmlEntitiesDecoder {

    public String decode(String text) {
        if (text == null) return "";

        return StringEscapeUtils.unescapeHtml4(text);
    }

    public String decodeBasic(String text) {
        if (text == null) return "";

        return text
                .replace("&#8230;", "...")
                .replace("&#8220;", "\"")  // 왼쪽 큰따옴표
                .replace("&#8221;", "\"")  // 오른쪽 큰따옴표
                .replace("&#8216;", "'")   // 왼쪽 작은따옴표
                .replace("&#8217;", "'")   // 오른쪽 작은따옴표
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}

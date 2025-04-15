package com.example.GateStatus.global.config.open;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FigureNameExtractor {

    // 한글 이름(2~4글자) + 공백 + 따옴표 패턴
    private static final Pattern NAME_QUOTE_PATTERN =
            Pattern.compile("([가-힣]{2,4})\\\\s+[\\\"']");

    // 따옴표 내용 + 공백 + 한글 이름(2~4글자) + 말했다/전했다 등 패턴
    private static final Pattern QUOTE_NAME_SAID_PATTERN =
            Pattern.compile("[\\\"'].*?[\\\"']\\\\s+([가-힣]{2,4})\\\\s+(말했|전했|밝혔|강조했|주장했|설명했)");

    // 문장 시작 + 한글 이름(2~4글자) + 공백 + 따옴표 패턴
    private static final Pattern START_NAME_QUOTE_PATTERN =
            Pattern.compile("^([가-힣]{2,4})\\\\s+[\\\"']");

    // 한글 이름(2~4글자) + 의원/장관/총리/대표 등 직함 패턴
    private static final Pattern NAME_TITLE_PATTERN =
            Pattern.compile("([가-힣]{2,4})\\\\s+(의원|장관|총리|대표|위원장|대통령|총장|원장)");


    /**
     * 컨텐츠에서 인물 이름 추출
     * 여러 패턴을 순차적으로 시도
     * @param content
     * @return
     */
    public String extractFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return "알 수 없음";
        }

        // 1. 이름 + 따옴표 패턴 (예: "윤석열 "발언내용"")
        Matcher matcher1 = NAME_QUOTE_PATTERN.matcher(content);
        if (matcher1.find()) {
            return matcher1.group(1);
        }

        // 2. 따옴표 + 이름 + 말했다/전했다 패턴
        Matcher matcher2 = QUOTE_NAME_SAID_PATTERN.matcher(content);
        if (matcher2.find()) {
            return matcher2.group(1);
        }

        // 3. 문장 시작 + 이름 + 따옴표 패턴
        Matcher matcher3 = START_NAME_QUOTE_PATTERN.matcher(content);
        if (matcher3.find()) {
            return matcher3.group(1);
        }

        // 4. 이름 + 직함 패턴
        Matcher matcher4 = NAME_TITLE_PATTERN.matcher(content);
        if (matcher4.find()) {
            return matcher4.group(1);
        }

        return "알 수 없음";
    }

    /**
     * 주어진 후보 이름들 중에서 컨텐츠에 언급된 이름 찾기
     * @param content
     * @param candidates
     * @return
     */
    public String extractFromCandidates(String content, String[] candidates) {
        if (content == null || content.isEmpty() || candidates == null || candidates.length == 0) {
            return "알 수 없음";
        }

        for (String candidate : candidates) {
            if (content.contains(candidate)) {
                return candidate;
            }
        }

        return "알 수 없음";
    }
}

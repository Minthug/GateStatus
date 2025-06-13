package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.example.GateStatus.global.config.open.FigureNameExtractor;
import com.example.GateStatus.global.config.open.HtmlEntitiesDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.compile;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatementApiMapper implements ApiMapper<String, List<StatementApiDTO>> {

    private final HtmlEntitiesDecoder htmlDecoder;
    private final FigureNameExtractor nameExtractor;


    // ==================== 상수 정의 ====================

    /**
     * 감성 분석용 긍정 키워드
     */
    private static final String[] POSITIVE_KEYWORDS = {
            "좋은", "발전", "성공", "긍정", "찬성", "지지", "개선", "향상", "효과적", "우수",
            "훌륭한", "멋진", "완벽한", "최고", "만족", "기쁜", "행복", "희망적", "밝은"
    };

    /**
     * 감성 분석용 부정 키워드
     */
    private static final String[] NEGATIVE_KEYWORDS = {
            "나쁜", "실패", "문제", "부정", "반대", "비판", "악화", "심각", "위험", "부족",
            "끔찍한", "최악", "실망", "걱정", "우려", "분노", "슬픈", "어려운", "힘든"
    };

    /**
     * 발언 유형 판단용 키워드 맵
     */
    private static final Map<String, String> TYPE_KEYWORDS = Map.of(
            "INTERVIEW", "인터뷰,대담,면담",
            "SPEECH", "연설,강연,발표,대회,축사",
            "ASSEMBLY", "국회,본회의,국정감사,국정질문",
            "COMMITTEE", "위원회,상임위,특별위,소위원회",
            "PRESS", "보도자료,기자회견,발표문,성명서",
            "DEBATE", "토론,논쟁,토론회,세미나",
            "SNS", "sns,트위터,페이스북,인스타그램,블로그"
    );

    @Override
    public List<StatementApiDTO> map(AssemblyApiResponse<String> response) {
        if (response == null || response.data() == null) {
            log.warn("API 응답이 null이거나 데이터가 없습니다");
            return Collections.emptyList();
        }

        try {
            String xmlData = response.data();
            log.debug("XML 데이터 파싱 시작, 길이: {} bytes", xmlData.length());

            List<StatementApiDTO> result = parseXmlData(xmlData);
            log.info("XML 파싱 완료: {}개 발언 정보 추출", result.size());

            return result;
        } catch (Exception e) {
            log.error("발언 정보 매핑 중 오류 발생", e);
            throw new ApiMappingException("발언 정보 매핑 중 오류 발생 ");
        }
    }

    /**
     * API 응답을 직접 StatementResponse로 변환 (빠른 조회용)
     * @param response API 응답
     * @return StatementResponse 리스트
     */
    public List<StatementResponse> mapToStatementResponses(AssemblyApiResponse<String> response) {
        List<StatementApiDTO> dtos = map(response);

        return dtos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== XML 파싱 메서드들 ====================

    /**
     * XML 응답을 파싱하여 StatementApiDTO 리스트로 반환
     * @param xmlData
     * @return
     */
    private List<StatementApiDTO> parseXmlData(String xmlData) {
        List<StatementApiDTO> result = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE 공격 방지를 위한 보안 설정
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

            NodeList rowList = doc.getElementsByTagName("row");
            log.debug("XML에서 {}개 row 발견", rowList.getLength());

            for (int i = 0; i < rowList.getLength(); i++) {
                Element row = (Element) rowList.item(i);
                StatementApiDTO dto = extractStatementFromRow(row);
                if (dto != null) {
                    result.add(dto);
                    log.trace("발언 정보 추출 성공: {}", dto.title());
                }
            }
        } catch (Exception e) {
            log.error("XML 파싱 실패: {}", e.getMessage());
            throw new ApiMappingException("XML 파싱 오류");
        }
        return result;
    }

    /**
     * XML Row 요소에서 필요한 정보 추출
     * @param row
     * @return
     */
    private StatementApiDTO extractStatementFromRow(Element row) {
        try {
            String title = getElementTextContent(row, "COMP_MAIN_TITLE");
            String content = getElementTextContent(row, "COMP_CONTENT");
            String regDate = getElementTextContent(row, "REG_DATE");

            title = htmlDecoder.decodeBasic(title);
            content = htmlDecoder.decodeBasic(content);

            if (isInvalidContent(title, content)) {
                log.debug("필수 필드 누락 또는 무효한 내용으로 발언 정보 건너뜀");
                return null;
            }

            String figureName = nameExtractor.extractFromContent(content);
            String typeCode = determineTypeCodeFromContent(content);
            LocalDate statementDate = parseDate(regDate);
            String originalUrl = generateOriginalUrl(regDate, title);

            return new StatementApiDTO(
                    title,
                    content,
                    statementDate,
                    "국회방송국",
                    originalUrl,
                    typeCode,
                    figureName,
                    "뉴스 기사"
            );
        } catch (Exception e) {
            log.warn("Xml Row에서 발언 정보 추출 실패", e);
            return null;
        }
    }

    /**
     * 내용 유효성 검증
     * @param title 제목
     * @param content 내용
     * @return 무효한 내용인지 여부
     */
    private boolean isInvalidContent(String title, String content) {
        return title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty() ||
                content.length() < 10;
    }

    // ==================== 데이터 변환 메서드들 ====================

    public StatementType convertToStatementType(String typeCode) {
        if (typeCode == null || typeCode.isEmpty()) {
            return StatementType.OTHER;
        }

        return switch (typeCode.toUpperCase()) {
            case "SPEECH" -> StatementType.SPEECH;
            case "INTERVIEW" -> StatementType.INTERVIEW;
            case "PRESS" -> StatementType.PRESS_RELEASE;
            case "DEBATE" -> StatementType.DEBATE;
            case "ASSEMBLY" -> StatementType.ASSEMBLY_SPEECH;
            case "COMMITTEE" -> StatementType.COMMITTEE_SPEECH;
            case "MEDIA" -> StatementType.MEDIA_COMMENT;
            case "SNS" -> StatementType.SOCIAL_MEDIA;
            default -> {
                log.debug("알 수 없는 발언 유형 코드: {}", typeCode);
                yield StatementType.OTHER;
            }
        };
    }


    /**
     * StatementApiDTO를 StatementResponse로 변환
     */
    private StatementResponse convertToResponse(StatementApiDTO dto) {

        Map<String, Object> nlpData = createBasicNlpData(dto.content());
        List<String> checkableItems = extractCheckableItems(dto.content());

        return new StatementResponse(
                UUID.randomUUID().toString(), // 임시 ID 생성
                null, // figureId는 나중에 설정
                dto.figureName(),
                dto.title(),
                dto.content(),
                dto.statementDate(),
                dto.source(),
                dto.context(),
                dto.originalUrl(),
                convertToStatementType(dto.typeCode()),
                null, // factCheckScore는 나중에 설정
                null, // factCheckResult는 나중에 설정
                checkableItems,
                nlpData,
                0, // viewCount 초기값
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ==================== 발언 유형 분석 메서드들 ====================

    private String determineTypeCodeFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return "OTHER";
        }

        String lowerContent = content.toLowerCase();

        for (Map.Entry<String, String> entry : TYPE_KEYWORDS.entrySet()) {
            String typeCode = entry.getKey();
            String[] keywords = entry.getValue().split(",");

            for (String keyword : keywords) {
                if (lowerContent.contains(keyword.trim())) {
                    log.trace("발언 유형 결정: {} (키워드: {})", typeCode, keyword);
                    return typeCode;
                }
            }
        }
        return "OTHER";
    }

    public StatementType analyzeContentForType(String content) {
        String typeCode = determineTypeCodeFromContent(content);
        return convertToStatementType(typeCode);
    }

    // ==================== NLP 분석 메서드들 ====================

    public Map<String, Object> createBasicNlpData(String content) {
        Map<String, Object> nlpData = new HashMap<>();

        if (content != null && !content.isEmpty()) {
            nlpData.put("checkableItems", extractCheckableItems(content));
            nlpData.put("keyPhrases", extractKeyPhrases(content));
            nlpData.put("sentiment", analyzeSentiment(content));
            nlpData.put("wordCount", content.length());
            nlpData.put("hasNumbers", content.matches(".*\\d+.*"));
            nlpData.put("hasStatistics", hasStatisticalContent(content));
            nlpData.put("complexity", calculateComplexity(content));
        }
        return nlpData;
    }

    private Map<String, Object> analyzeSentiment(String content) {
        Map<String, Object> sentiment = new HashMap<>();

        int positiveCount = countWords(content, new String[]{"좋은", "발전", "성공", "긍정", "찬성", "지지"});
        int negativeCount = countWords(content, new String[]{"나쁜", "실패", "문제", "부정", "반대", "비판"});
        int totalSentimentWords = positiveCount + negativeCount;

        double score = 0.5; // 중립 기본값
        if (totalSentimentWords > 0) {
            score = (double) positiveCount / totalSentimentWords;
        }

        String classification;
        if (score > 0.6) {
            classification = "POSITIVE";
        } else if (score < 0.4) {
            classification = "NEGATIVE";
        } else {
            classification = "NEUTRAL";
        }

        sentiment.put("score", Math.round(score * 100.0) / 100.0);
        sentiment.put("positive", positiveCount);
        sentiment.put("negative", negativeCount);
        sentiment.put("classification", classification);
        sentiment.put("confidence", calculateSentimentConfidence(totalSentimentWords, content.length()));

        return sentiment;
    }

    private double calculateSentimentConfidence(int sentimentWords, int totalLength) {
        if (totalLength == 0) return 0.0;

        double density = (double) sentimentWords / (totalLength / 100.0);
        return Math.min(1.0, density / 5.0);
    }

    private List<String> extractKeyPhrases(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> phrases = new ArrayList<>();

        extractQuotedPhrase(content, phrases);

        extractDelimitedPhrase(content, phrases);

        return phrases.stream()
                .filter(phrase -> phrase.length() > 5 && phrase.length() < 50)
                .filter(phrase -> !phrase.matches(".*\\d{4}-\\d{2}-\\d{2}.*"))
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    private void extractQuotedPhrase(String content, List<String> phrases) {
        String[] quotedParts = content.split("[\"']");
        for (int i = 0; i < quotedParts.length; i += 2) {
            String phrase = quotedParts[i].trim();
            if (!phrase.isEmpty()) {
                phrases.add(phrase);
            }
        }
    }

    private void extractDelimitedPhrase(String content, List<String> phrases) {
        String[] parts = content.split("[,。.!?;]");
        for (String part : parts) {
            part = part.trim();
            if (part.length() > 5  && part.length() < 50) {
                phrases.add(part);
            }
        }
    }

    protected List<String> extractCheckableItems(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> items = new ArrayList<>();
        String[] sentences = content.split("[.!?。]");

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() < 15 || sentence.length() > 200) continue; // 적절한 길이만

            if (isCheckableSentence(sentence)) {
                String formattedSentence = sentence + (sentence.endsWith(".") ? "" : ".");
                items.add(formattedSentence);

                if (items.size() >= 5) break; // 최대 5개
            }
        }

        return items;
    }

    private boolean isCheckableSentence(String sentence) {
        // 숫자나 통계가 포함된 문장
        if (sentence.matches(".*\\d+.*")) return true;

        // 특정 단위가 포함된 문장
        if (sentence.matches(".*(퍼센트|%|억원|조원|만명|천명|개|건|년|월).*")) return true;

        // 인용이나 발언이 포함된 문장
        if (sentence.matches(".*(라고 밝혔|라고 주장|라고 말했|것으로 나타났|것으로 조사|발표했|보고했).*")) return true;

        // 단정적인 표현이 포함된 문장
        if (sentence.matches(".*(이다|였다|했다|될 것이다|할 예정이다).*")) return true;

        return false;
    }

    // ==================== 유틸리티 메서드들 ====================

    /**
     * 원본 URL 생성
     * @param regDate
     * @param title
     * @return
     */
    private String generateOriginalUrl(String regDate, String title) {
        // 날짜에서 특수문자 제거
        String dateStr = regDate.replaceAll("[^0-9]", "");

        // 제목에서 URL 안전한 문자열 생성(50자 제한)
        String slugTitle;
        if (title.length() > 50) {
            slugTitle = title.substring(0, 50);
        } else {
            slugTitle = title;
        }

        slugTitle = slugTitle.replaceAll("[^a-zA-Z0-9가-힣]", "-")
                .replaceAll("-{2,}", "-");

        return "https://assembly.news.go.kr/news/" + dateStr + "/" + slugTitle;
    }


    /**
     * 날짜 문자열 파싱
     * @param dateStr
     * @return
     */
    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr.split(" ")[0]);
        } catch (Exception e) {
            log.error("날짜 변환 실패: {}", dateStr, e);
            return LocalDate.now();
        }
    }

    /**
     * Element에서 지정된 태의 텍스트 컨텐츠 가져오기
     * @param element
     * @param tagName
     * @return
     */
    private String getElementTextContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }

        return "";
    }

    private int countWords(String text, String[] words) {
        int count = 0;
        String lowerText = text.toLowerCase();
        for (String word : words) {
            int index = 0;
            while ((index = lowerText.indexOf(word.toLowerCase(), index)) != -1) {
                count++;
                index += word.length();
            }
        }

        return count;
    }


    /**
     * API의 발언 유형 코드를 애플리케이션 StatementType으로 변환
     * @param typeCode
     * @return
     */
    public StatementType determineStatementType(String typeCode) {
        switch (typeCode) {
            case "SPEECH":
                return StatementType.SPEECH;
            case "INTERVIEW":
                return StatementType.INTERVIEW;
            case "PRESS":
                return StatementType.PRESS_RELEASE;
            case "DEBATE":
                return StatementType.DEBATE;
            case "ASSEMBLY":
                return StatementType.ASSEMBLY_SPEECH;
            case "COMMITTEE":
                return StatementType.COMMITTEE_SPEECH;
            case "MEDIA":
                return StatementType.MEDIA_COMMENT;
            case "SNS":
                return StatementType.SOCIAL_MEDIA;
            default:
                return StatementType.OTHER;
        }
    }
}

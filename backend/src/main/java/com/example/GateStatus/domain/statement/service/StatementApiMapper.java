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

    @Override
    public List<StatementApiDTO> map(AssemblyApiResponse<String> response) {
        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }

        try {
            String xmlData = response.data();
            return parseXmlData(xmlData);
        } catch (Exception e) {
            log.error("발언 정보 매핑 중 오류 발생", e);
            throw new ApiMappingException("발언 정보 매핑 중 오류 발생 ");
        }
    }

    /**
     * XML 응답을 파싱하여 StatementApiDTO 리스트로 반환
     * @param xmlData
     * @return
     */
    private List<StatementApiDTO> parseXmlData(String xmlData) {
        List<StatementApiDTO> result = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlData)));

            NodeList rowList = doc.getElementsByTagName("row");

            for (int i = 0; i < rowList.getLength(); i++) {
                Element row = (Element) rowList.item(i);
                StatementApiDTO dto = extractStatementFromRow(row);
                if (dto != null) {
                    result.add(dto);
                }
            }
        } catch (Exception e) {
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
     * 컨텐츠 기반으로 발언 유형 추정
     * @param content
     * @return
     */
    private String determineTypeCodeFromContent(String content) {
        String lowerContent = content.toLowerCase();

        if (lowerContent.contains("인터뷰") || lowerContent.contains("대담")) {
            return "INTERVIEW";
        } else if (lowerContent.contains("연설") || lowerContent.contains("대회")) {
            return "SPEECH";
        } else if (lowerContent.contains("국회") || lowerContent.contains("본회의")) {
            return "ASSEMBLY";
        } else if (lowerContent.contains("위원회") || lowerContent.contains("상임위")) {
            return "COMMITTEE";
        } else if (lowerContent.contains("보도자료") || lowerContent.contains("발표")) {
            return "PRESS";
        } else if (lowerContent.contains("토론") || lowerContent.contains("논쟁")) {
            return "DEBATE";
        } else if (lowerContent.contains("sns") || lowerContent.contains("트위터")
                || lowerContent.contains("페이스북") || lowerContent.contains("인스타그램")) {
            return "SNS";
        } else {
            return "OTHER";
        }
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

    public List<StatementResponse> mapToStatementResponses(AssemblyApiResponse<String> response) {
        List<StatementApiDTO> dtos = map(response);

        return dtos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * StatementApiDTO를 StatementResponse로 변환
     */
    private StatementResponse convertToResponse(StatementApiDTO dto) {

        StatementType statementType = determineStatementType(dto.typeCode());

        Map<String, Object> nlpData = new HashMap<>();


        // 기본적인 NLP 데이터 초기화 (필요에 따라 조정)
        nlpData.put("checkableItems", extractCheckableItems(dto.content()));
        nlpData.put("keyPhrases", extractKeyPhrases(dto.content()));
        nlpData.put("sentiment", analyzeSentiment(dto.content()));

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
                statementType,
                null, // factCheckScore는 나중에 설정
                null, // factCheckResult는 나중에 설정
                extractCheckableItems(dto.content()),
                nlpData,
                0, // viewCount 초기값
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Map<String, Object> analyzeSentiment(String content) {
        Map<String, Object> sentiment = new HashMap<>();

        int positiveCount = countWords(content, new String[]{"좋은", "발전", "성공", "긍정", "찬성", "지지"});
        int negativeCount = countWords(content, new String[]{"나쁜", "실패", "문제", "부정", "반대", "비판"});

        double score = 0.5; // 중립 기본값

        if (positiveCount + negativeCount > 0) {
            score = (double) positiveCount / (positiveCount + negativeCount);
        }

        sentiment.put("score", score);
        sentiment.put("positive", positiveCount);
        sentiment.put("negative", negativeCount);

        return sentiment;
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

    private List<String> extractKeyPhrases(String content) {
        List<String> phrases = new ArrayList<>();

        String[] parts = content.split("[,\"']");
        for (String part : parts) {
            part = part.trim();
            if (part.length() > 5 && part.length() < 50) {
                phrases.add(part);
            }
        }
        return phrases.stream().limit(5).collect(Collectors.toList());
    }

    protected List<String> extractCheckableItems(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> items = new ArrayList<>();

        String[] sentences = content.split("\\. ");
        for (String sentence : sentences) {
            if (sentence.matches(".*\\d+.*") ||
                    sentence.contains("이다") ||
                    sentence.contains("했다") ||
                    sentence.contains("라고 말했") ||
                    sentence.contains("주장")) {
                items.add(sentence.trim() + (sentence.endsWith(".") ? "" : "."));
            }
        }

        return items.stream().limit(3).collect(Collectors.toList());
    }

    private StatementType determineStatementType(String s) {
        return null;
    }
}

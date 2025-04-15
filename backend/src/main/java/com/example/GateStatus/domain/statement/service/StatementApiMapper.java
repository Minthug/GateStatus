package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.global.config.exception.ApiMappingException;
import com.example.GateStatus.global.config.open.ApiMapper;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatementApiMapper implements ApiMapper<String, List<StatementApiDTO>> {

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

            title = decodeHtmlEntities(title);
            content = decodeHtmlEntities(content);

            String figureName = extractFigureNameFromContent(content);
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

    private LocalDate parseDate(String regDate) {
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
}

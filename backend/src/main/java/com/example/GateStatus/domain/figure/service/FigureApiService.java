package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import com.example.GateStatus.domain.figure.service.response.FigureMapper;
import com.example.GateStatus.global.config.exception.ApiDataRetrievalException;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureApiService {

    private final WebClient webClient;
    private final FigureApiMapper apiMapper;
    private final FigureRepository figureRepository;
    private final FigureMapper figureMapper;


    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apiKey;

    @Value("${spring.openapi.assembly.figure-api-path}")
    private String figureApiPath;


    @Transactional
    public Figure syncFigureInfoByName(String figureName) {
        FigureInfoDTO info = fetchFigureInfoFromApi(figureName);

        if (info == null) {
            throw new EntityNotFoundException("해당 이름의 정치인을 찾을 수 없습니다");
        }

        Figure figure = figureRepository.findByName(figureName)
                .orElseGet(() -> Figure.builder()
                        .name(figureName)
                        .figureType(FigureType.POLITICIAN)
                        .viewCount(0L)
                        .build());

        figureMapper.updateFigureFromDTO(figure, info);

        return figureRepository.save(figure);
    }

    private FigureInfoDTO fetchFigureInfoFromApi(String figureName) {
        try {
            log.info("국회의원 정보 API 호출 시작: {}", figureName);

            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("HG_NM", figureName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {
                    })
                    .block();

            log.info("API 응답 데이터:", apiResponse);
            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);

            log.info("전체 국회의원 정보 API 호출 완료: {}명", figures.size());

            if (figures.isEmpty()) {
                return null;
            }

            return figures.get(0);
        } catch (Exception e) {
            throw new ApiDataRetrievalException("국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    /**
     * 모든 국회의원 정보를 동기화 합니다
     *
     * @return
     */
//    @Transactional
//    public int syncAllFigures() {
//        try {
//            log.info("모든 국회의원 정보 동기화 시작");
//            List<FigureInfoDTO> allFigures = fetchAllFiguresFromApi();
//            log.info("가져온 국회의원 수: {}", allFigures.size());
//
//            int count = 0;
//
//            for (FigureInfoDTO infoDTO : allFigures) {
//                try {
//                    Figure figure = figureRepository.findByName(infoDTO.name())
//                            .orElseGet(() -> Figure.builder()
//                                    .name(infoDTO.name())
//                                    .figureType(FigureType.POLITICIAN)
//                                    .viewCount(0L)
//                                    .build());
//
//                    figureMapper.updateFigureFromDTO(figure, infoDTO);
//                    figureRepository.save(figure);
//                    count++;
//                } catch (Exception e) {
//                    log.error("국회의원 동기화 중 오류 발생: {} - {}", infoDTO.name(), e.getMessage(), e);
//                }
//            }
//            log.info("국회의원 정보 동기화 완료: {}", count);
//            return count;
//        } catch (Exception e) {
//            throw new ApiDataRetrievalException("전체 국회의원 정보를 동기화 하는 중 오류 발생");
//        }
//    }
    @Transactional
    public int syncAllFigures() {
        try {
            log.info("모든 국회의원 정보 동기화 시작");

            // API 호출 부분 시도
            List<FigureInfoDTO> allFigures;
            try {
                allFigures = fetchAllFiguresFromApi();
                log.info("API에서 가져온 국회의원 수: {}", allFigures.size());
            } catch (Exception e) {
                log.error("API 호출 중 오류 발생: {}", e.getMessage(), e);
                throw new ApiDataRetrievalException("API에서 국회의원 정보를 가져오는 중 오류 발생: " + e.getMessage());
            }

            // 확인을 위해 첫 번째 DTO 로깅
            if (!allFigures.isEmpty()) {
                log.info("첫 번째 국회의원 정보: {}", allFigures.get(0));
            } else {
                log.warn("API에서 가져온 국회의원 정보가 없습니다");
                return 0;
            }

            int count = 0;
            // 각 국회의원 정보 처리 및 저장
            for (FigureInfoDTO infoDTO : allFigures) {
                try {
                    log.info("국회의원 정보 처리 중: {}", infoDTO.name());

                    Figure figure = figureRepository.findByName(infoDTO.name())
                            .orElseGet(() -> {
                                log.info("새 국회의원 생성: {}", infoDTO.name());
                                return Figure.builder()
                                        .name(infoDTO.name())
                                        .figureType(FigureType.POLITICIAN)
                                        .viewCount(0L)
                                        .build();
                            });

                    // 매퍼 호출 부분 try-catch로 감싸기
                    try {
                        figureMapper.updateFigureFromDTO(figure, infoDTO);
                    } catch (Exception e) {
                        log.error("매퍼 처리 중 오류 발생: {} - {}", infoDTO.name(), e.getMessage(), e);
                        continue; // 이 국회의원은 건너뛰고 다음으로 진행
                    }

                    // 저장 시도 부분
                    try {
                        figureRepository.save(figure);
                        count++;
                        log.info("국회의원 저장 성공: {}", infoDTO.name());
                    } catch (Exception e) {
                        log.error("국회의원 저장 중 오류 발생: {} - {}", infoDTO.name(), e.getMessage(), e);
                    }
                } catch (Exception e) {
                    log.error("국회의원 처리 중 전체 오류: {} - {}", infoDTO.name(), e.getMessage(), e);
                }
            }

            log.info("국회의원 정보 동기화 완료: {}명 중 {}명 성공", allFigures.size(), count);
            return count;
        } catch (Exception e) {
            log.error("전체 국회의원 동기화 중 오류 발생: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("전체 국회의원 정보를 동기화 하는 중 오류 발생");
        }
    }

    /**
     * 모든 국회의원 정보를 API에서 가져옵니다
     *
     * @return
     */
//    private List<FigureInfoDTO> fetchAllFiguresFromApi() {
//        try {
//            log.info("전체 국회의원 정보 API 호출 시작");
//
//            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
//                    .uri(uriBuilder -> uriBuilder
//                            .path(figureApiPath)
//                            .queryParam("key", apiKey)
//                            .build())
//                    .retrieve()
//                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {})
//                    .block();
//
//            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);
//            log.info("전체 국회의원 정보 API 호출 완료: {}명", figures.size());
//
//            return figures;
//        } catch (Exception e) {
//            throw new ApiDataRetrievalException("전체 국회의원 정보를 가져오는 중 오류 발생");
//        }
//    }
    private List<FigureInfoDTO> fetchAllFiguresFromApi() {
        try {
            log.info("전체 국회의원 정보 API 호출 시작");

            try {
                String xmlResponse = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(figureApiPath)
                                .queryParam("key", apiKey)
                                .build())
                        .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.info("API XML 응답 수신: {}", xmlResponse != null ? xmlResponse.substring(0, Math.min(1000, xmlResponse.length())) : "null");

                List<FigureInfoDTO> figures = parseXmlToFigureInfoList(xmlResponse);
                log.info("국회의원 정보 파싱 완료: {} 명", figures.size());

                return figures;
            } catch (Exception e) {
                log.error("API 호출 자체에서 오류 발생: {}", e.getMessage(), e);
                throw new ApiDataRetrievalException("API 호출 실패: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("전체 API 처리 과정에서 오류 발생: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("전체 국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    /**
     * 특정 정당 소속 국회의원 정보를 동기화합니다
     *
     * @param partyName
     * @return
     */
    @Transactional
    public int syncFigureByParty(String partyName) {
        try {
            log.info("{}당 소속 국회의원 정보 동기화 시작", partyName);
            List<FigureInfoDTO> partyFigures = fetchAllFiguresByPartyFromApi(partyName);
            int count = 0;

            for (FigureInfoDTO figureInfoDTO : partyFigures) {
                try {
                    Figure figure = figureRepository.findByName(figureInfoDTO.name())
                            .orElseGet(() -> Figure.builder()
                                    .name(figureInfoDTO.name())
                                    .figureType(FigureType.POLITICIAN)
                                    .viewCount(0L)
                                    .build());

                    figureMapper.updateFigureFromDTO(figure, figureInfoDTO);

                    figureRepository.save(figure);
                    count++;
                    log.debug("국회의원 정보 동기화 완료: {}", figureInfoDTO.name());

                } catch (Exception e) {
                    log.error("국회의원 동기화 중 오류 발생: {}", figureInfoDTO.name(), e);
                    // 개별 국회의원 동기화 오류는 무시하고 계속 진행
                }
            }

            log.info("{}당 소속 국회의원 정보 동기화 완료: {}명", partyName, count);
            return count;
        } catch (Exception e) {
            log.error("정당별 국회의원 동기화 중 오류 발생: {}", partyName, e);
            throw new ApiDataRetrievalException("정당별 국회의원 정보를 동기화하는 중 오류 발생");
        }
    }

    /**
     * 특정 정당 소속 국회의원 정보를 API에서 가져옵니다.
     *
     * @param partyName 정당 이름
     * @return 국회의원 정보 DTO 목록
     */
    private List<FigureInfoDTO> fetchAllFiguresByPartyFromApi(String partyName) {
        try {
            log.info("{}당 소속 국회의원 정보 API 호출 시작", partyName);

            AssemblyApiResponse<JsonNode> apiResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(figureApiPath)
                            .queryParam("KEY", apiKey)
                            .queryParam("POLY_NM", partyName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AssemblyApiResponse<JsonNode>>() {
                    })
                    .block();

            List<FigureInfoDTO> figures = apiMapper.map(apiResponse);
            log.info("{}당 소속 국회의원 정보 API 호출 완료: {}명", partyName, figures.size());

            return figures;
        } catch (Exception e) {

            log.error("정당별 국회의원 정보 조회 중 오류 발생: {}", partyName, e);
            throw new ApiDataRetrievalException("정당별 국회의원 정보를 가져오는 중 오류 발생");
        }
    }

    private List<FigureInfoDTO> parseXmlToFigureInfoList(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlResponse)));

            document.getDocumentElement().normalize();

            XPath xpath = XPathFactory.newInstance().newXPath();

            String expression = "//row";
            NodeList nodeList = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

            List<FigureInfoDTO> result = new ArrayList<>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // 필수 필드 추출
                    String figureId = getElementValue(element, "MONA_CD");
                    String name = getElementValue(element, "HG_NM");

                    // 소속 정당 처리 (문자열에서 열거형으로 변환)
                    String partyNameStr = getElementValue(element, "POLY_NM");
                    FigureParty partyName = convertToFigureParty(partyNameStr);

                    // 기본 정보 추출
                    String englishName = getElementValue(element, "ENG_NM");
                    String birth = getElementValue(element, "BTH_DATE");
                    String constituency = getElementValue(element, "ORIG_NM");
                    String committeeName = getElementValue(element, "CMIT_NM");
                    String committeePosition = getElementValue(element, "CMITS_NM");
                    String electedCount = getElementValue(element, "REELE_GBN_NM");
                    String electedDate = getElementValue(element, "UNITS_NM");
                    String reelection = getElementValue(element, "REELE_GBN_NM");
                    String profileUrl = getElementValue(element, "IMAGE_URL"); // 또는 "PROFILE_URL"

                    // 연락처 정보
                    String email = getElementValue(element, "E_MAIL");
                    String homepage = getElementValue(element, "HOMEPAGE");
                    String blog = getElementValue(element, "BLOG");
                    String facebook = getElementValue(element, "FACEBOOK");

                    // 학력 및 경력 정보 (여러 항목이 있을 수 있음)
                    List<String> education = new ArrayList<>();
                    List<String> career = new ArrayList<>();

                    // 학력 태그가 여러 개 있을 수 있음 (EDU1, EDU2, EDU3 등)
                    addNonEmptyValue(education, getElementValue(element, "EDU"));
                    addNonEmptyValue(education, getElementValue(element, "EDU1"));
                    addNonEmptyValue(education, getElementValue(element, "EDU2"));
                    addNonEmptyValue(education, getElementValue(element, "EDU3"));

                    // 경력 태그가 여러 개 있을 수 있음 (CAREER1, CAREER2, CAREER3 등)
                    addNonEmptyValue(career, getElementValue(element, "CAREER"));
                    addNonEmptyValue(career, getElementValue(element, "CAREER1"));
                    addNonEmptyValue(career, getElementValue(element, "CAREER2"));
                    addNonEmptyValue(career, getElementValue(element, "CAREER3"));

                    // 전체 경력 문자열에서 개별 항목 분리 (세미콜론이나 다른 구분자로 구분되어 있을 수 있음)
                    splitAndAddToList(education, getElementValue(element, "EDU_CAREERS"));
                    splitAndAddToList(career, getElementValue(element, "MEM_CAREERS"));

                    // DTO 생성
                    FigureInfoDTO dto = new FigureInfoDTO(
                            figureId, name, englishName, birth, partyName, constituency,
                            committeeName, committeePosition, electedCount, electedDate,
                            reelection, profileUrl, education, career,
                            email, homepage, blog, facebook
                    );

                    result.add(dto);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("XML 파싱 중 오류 발생: {}", e.getMessage(), e);
            throw new ApiDataRetrievalException("XML 파싱 실패: " + e.getMessage());
        }
    }


    // XML 요소에서 단일 값 추출 (안전하게 처리)
    private String getElementValue(Element element, String tagName) {
        try {
            NodeList nodeList = element.getElementsByTagName(tagName);
            if (nodeList != null && nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                if (node != null) {
                    return node.getTextContent().trim();
                }
            }
        } catch (Exception e) {
            log.debug("태그 {} 추출 중 오류: {}", tagName, e.getMessage());
        }
        return null;
    }

    // 빈 값이 아닌 경우에만 리스트에 추가
    private void addNonEmptyValue(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(value.trim());
        }
    }

    // 문자열을 구분자로 분리하여 리스트에 추가
    private void splitAndAddToList(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            // 여러 가능한 구분자로 시도 (세미콜론, 쉼표, 줄바꿈 등)
            String[] items = value.split("[;,\n]+");
            for (String item : items) {
                if (item != null && !item.trim().isEmpty()) {
                    list.add(item.trim());
                }
            }
        }
    }

    // 문자열을 FigureParty 열거형으로 변환
    private FigureParty convertToFigureParty(String partyName) {
        if (partyName == null || partyName.isEmpty()) {
            return null;
        }

        // 정당명 매핑 (실제 정당 이름과 열거형 값 매핑이 필요)
        try {
            // 여기에 특정 문자열을 FigureParty 열거형으로 매핑하는 로직 추가
            // 예시: if(partyName.contains("더불어민주당")) return FigureParty.DEMOCRATIC;

            // 기본적으로 문자열 그대로 변환 시도
            return FigureParty.valueOf(partyName.toUpperCase().replace(" ", "_"));
        } catch (Exception e) {
            log.warn("정당명 변환 실패: {}", partyName);
            return null; // 또는 기본값 반환
        }
    }
}

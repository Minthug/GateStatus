package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.example.GateStatus.global.config.redis.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1/statements/direct")
@RestController
public class DirectStatementController {

    private final RedisCacheService cacheService;



    /**
     * XML 응답에서 결과 메시지 추출
     * @param xmlResponse
     * @return
     */
    private String extractResultMessage(String xmlResponse) {
        if (xmlResponse.contains("<MESSAGE>")) {
            int start = xmlResponse.indexOf("<MESSAGE>") + "<MESSAGE>".length();
            int end = xmlResponse.indexOf("</MESSAGE>");
            if (start > 0 && end > start) {
                return xmlResponse.substring(start, end);
            }
        }
        return "처리 중 오류가 발생했습니다";
    }

    /**
     * XML 응답에서 결과 코드 추출
     * @param xmlResponse
     * @return
     */
    private String extractResultCode(String xmlResponse) {
        if (xmlResponse.contains("<CODE>")) {
            int start = xmlResponse.indexOf("<CODE>") + "<CODE>".length();
            int end = xmlResponse.indexOf("</CODE>");
            if (start > 0 && end > start) {
                return xmlResponse.substring(start, end);
            }
        }
        return "99";
    }
}

package com.example.GateStatus.global.openAi;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OpenAiClient {

    private final OpenAiService service;

    public OpenAiClient(@Value("${spring.openai.api.key}") String apiKey) {
        this.service = new OpenAiService(apiKey, Duration.ofSeconds(60));
    }

    /**
     * 기사에서 특정 인물의 발언을 추출
     */
    public String extractQuote(String articleText, String figureName) {

    }
}

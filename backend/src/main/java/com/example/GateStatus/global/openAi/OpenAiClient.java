package com.example.GateStatus.global.openAi;

import com.example.GateStatus.domain.statement.entity.StatementType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        ChatMessage systemMessage = new ChatMessage("system",
                "당신은 기사에서 특정 인물의 발언을 정확히 추출하는 AI입니다. " +
                        "직접 인용된 발언만 추출하고, 기자의 해석이나 간접 인용은 제외하세요. " +
                        "추출한 발언은 큰따옴표 없이 반환하세요.");

        ChatMessage userMessage = new ChatMessage("user", String.format("다음 기사에서 %s의 직접 발언을 모두 추출해주세요:\n\n%s",
                figureName, articleText));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.0)
                .build();

        ChatCompletionResult result = service.createChatCompletion(chatCompletionRequest);

        return result.getChoices().get(0).getMessage().getContent();

    }

    /**
     * 발언 내용 분석하여 유형 분류
     */
    public StatementType classifyStatement(String statement) {
        ChatMessage systemMessage = new ChatMessage("system",
                "당신은 정치인의 발언을 분석하여 유형을 분류하는 AI입니다. " +
                        "가능한 유형은 SPEECH(연설), INTERVIEW(인터뷰), PRESS_RELEASE(보도자료), " +
                        "DEBATE(토론), ASSEMBLY_SPEECH(국회연설), COMMITTEE_SPEECH(위원회발언), " +
                        "MEDIA_COMMENT(언론논평), SOCIAL_MEDIA(SNS), OTHER(기타) 입니다. " +
                        "결과는 유형만 대문자로 정확히 반환하세요."
        );

        ChatMessage userMessage = new ChatMessage("user", "다음 발언의 유형을 분류해주세요:\n\n" + statement);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.0)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);

        String type = result.getChoices().get(0).getMessage().getContent().trim();
        try {
            return StatementType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return StatementType.OTHER;
        }
    }


    public Map<String, Double> analyzeSentiment(String statement) {
        ChatMessage systemMessage = new ChatMessage("system",
                "당신은 발언의 감성을 분석하는 AI입니다. " +
                        "발언을 분석하여 긍정(POSITIVE), 부정(NEGATIVE), 중립(NEUTRAL)의 확률을 " +
                        "점수로 반환하세요. 세 점수의 합은 1.0이 되어야 합니다. " +
                        "JSON 형식으로 {\"POSITIVE\": 0.3, \"NEGATIVE\": 0.2, \"NEUTRAL\": 0.5}와 같이 응답하세요."
        );

        ChatMessage userMessage = new ChatMessage("user",
                "다음 발언의 감성을 분석해주세요:\n\n" + statement);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.0)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);

        String jsonString = result.getChoices().get(0).getMessage().getContent();

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            Map<String, Double> fallback = new HashMap<>();
            fallback.put("POSITIVE", 0.0);
            fallback.put("NEGATIVE", 0.0);
            fallback.put("NEUTRAL", 1.0);
            return fallback;
        }
    }

    public List<String> extractKeywords(String statement) {
        ChatMessage systemMessage = new ChatMessage("system",
                "당신은 발언에서 핵심 키워드를 추출하는 AI입니다. " +
                        "발언을 분석하여 가장 중요한 5-10개의 키워드를 추출하세요. " +
                        "결과는 [\"키워드1\", \"키워드2\", ...] 형식의 JSON 배열로 반환하세요.");

        ChatMessage userMessage = new ChatMessage("user",
                "다음 발언에서 핵심 키워드를 추출해주세요:\n\n" + statement);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.0)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);

        String jsonString = result.getChoices().get(0).getMessage().getContent();

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     *  발언 요약
     * @param statement
     * @return
     */
    public String summarizeStatement(String statement) {
        ChatMessage systemMessage = new ChatMessage("system",
                "당신은 정치인의 발언을 간결하게 요약하는 AI입니다. " +
                        "발언의 핵심 내용을 1-2문장으로 요약하세요. " +
                        "원래 발언의 핵심 메시지와 논조를 유지하되, 간결하게 요약하세요."
        );

        ChatMessage userMessage = new ChatMessage("user",
                "다음 발언을 요약해주세요:\n\n" + statement
        );

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.3)
                .maxTokens(100)
                .build();

        ChatCompletionResult result = service.createChatCompletion(chatCompletionRequest);

        return result.getChoices().get(0).getMessage().getContent();
    }

    /**
     * 발언 팩트체크 지원
     */
    public FactCheckResult factCheckStatement(String statement) {
        ChatMessage systemMessage = new ChatMessage("system",
                "당신은 정치인의 발언을 팩트체크하는 AI입니다. " +
                        "발언에 포함된 사실 주장을 식별하고, 그 주장이 검증 가능한지 평가하세요. " +
                        "결과는 JSON 형식으로 {\"score\": 75, \"explanation\": \"설명...\", \"checkableItems\": [\"항목1\", \"항목2\"]} 형태로 반환하세요. " +
                        "score는 0-100 사이의 팩트체크 점수(높을수록 더 많은 항목이 검증 가능)입니다."
        );

        ChatMessage userMessage = new ChatMessage("user",
                "다음 발언을 팩트체크해주세요:\n\n" + statement
        );

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.0)
                .build();

        ChatCompletionResult result = service.createChatCompletion(chatCompletionRequest);

        String jsonString = result.getChoices().get(0).getMessage().getContent();

        try {
            // Jackson ObjectMapper 사용
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, FactCheckResult.class);
        } catch (Exception e) {
            return new FactCheckResult(50, "팩트체크 처리 중 오류가 발생했습니다.", List.of());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class FactCheckResult {
        private int score;
        private String explanation;
        private List<String> checkableItems;
    }
}

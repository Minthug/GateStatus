package com.example.GateStatus.global.openAi;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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


}

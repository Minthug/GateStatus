package com.example.GateStatus.global.config.open;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${news.api.naver.base-url}")
    private String naverBaseUrl;

    @Value("${news.api.naver.client-id}")
    private String naverClientId;
    @Value("${news.api.naver.client-secret}")
    private String naverClientSecret;

    @Bean
    public WebClient assemblyWebClient() {
        return WebClient.builder()
                .baseUrl("https://open.assembly.go.kr/portal/openapi")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 버퍼 크기 설정 (2MB)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().jackson2SmileDecoder(
                                new Jackson2JsonDecoder(new ObjectMapper(), MediaType.APPLICATION_JSON)))
                        .build())
                .filter(ExchangeFilterFunction.ofRequestProcessor(
                        clientRequest -> {
                            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                            return Mono.just(clientRequest);
                        }
                ))
                .build();
    }


    @Bean
    public WebClient naverWebClient(WebClient.Builder builder) {
        log.info("네이버 WebClient 설정 - ClientId: {}", naverClientId.substring(0, 4) + "****");

        return builder
                .baseUrl(naverBaseUrl)
                .defaultHeader("X-Naver-Client-Id", naverClientId)
                .defaultHeader("X-Naver-Client-Secret", naverClientSecret)
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10 MB
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("네이버 API 요청: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> {
                if (name.startsWith("X-Naver")) {
                    log.debug("헤더: {} = {}", name,
                            name.contains("Secret") ? "****" : values.get(0).substring(0, 4) + "****");
                }
            });

            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("네이버 API 응답: {}", clientResponse.statusCode());
            if (clientResponse.statusCode().isError()) {
                return clientResponse.bodyToMono(String.class)
                        .doOnNext(body -> log.error("네이버 API 에러 응답: {}", body))
                        .then(Mono.just(clientResponse));
            }
            return Mono.just(clientResponse);
        });
    }
}
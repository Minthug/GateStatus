package com.example.GateStatus.domain.news;

import com.example.GateStatus.domain.news.repository.NewsRepository;
import com.example.GateStatus.domain.news.service.NewsApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsDataInitializer {

    private final NewsApiService newsApiService;
    private final NewsRepository newsRepository;

    @Value("${news.init.enabled:false}")
    private boolean initEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeNewsData() {
        if (!initEnabled) {
            log.info("뉴스 초기 데이터 수집 비활성화됨");
            return;
        }

        long existingCount = newsRepository.count();
        if (existingCount > 0) {
            log.info("이미 뉴스 데이터가 존재합니다: {}건", existingCount);
            return;
        }

        log.info("뉴스 초기 데이터 수집 시작");

        try {
            List<String> keywords = List.of(
                    "국회", "정치", "대통령", "법안", "정책",
                    "여당", "야당", "선거", "국정감사");

            for (String keyword : keywords) {
                Thread.sleep(1000);
                newsApiService.searchNaverNews(keyword, 10, 1, "date");
                log.info("키워드 '{}' 뉴스 수집 완료", keyword);
            }

            log.info("뉴스 초기 데이터 수집 완료");

        } catch (Exception e) {
            log.error("초기 데이터 수집 실패", e);
        }
    }
}

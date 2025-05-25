//package com.example.GateStatus.domain.news.service;
//
//import com.example.GateStatus.domain.figure.repository.FigureRepository;
//import com.example.GateStatus.domain.issue.service.IssueService;
//import com.example.GateStatus.domain.news.NewsDocument;
//import com.example.GateStatus.domain.news.repository.NewsRepository;
//import com.example.GateStatus.global.openAi.OpenAiClient;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.regex.Pattern;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class NewsIssueIntegrationService {
//
//    private final NewsRepository newsRepository;
//    private final IssueService issueService;
//    private double similarityThreshold;
//
//
//    /**
//     * 뉴스를 분석하여 이슈로 변환하거나 연결
//     * 매시간 30분에 연결
//     */
//    @Scheduled(cron = "0 30 * * * *")
//    @Transactional
//    public void processNewsToIssues() {
//
//        log.info("뉴스-이슈 통합 처리 시작");
//
//        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
//        List<NewsDocument> unprocessedNews = newsRepository.findByProce(cutoff);
//
//        log.info("처리할 뉴스 {}건 발견", unprocessedNews.size());
//
//        int successCount = 0;
//        int errorCount = 0;
//
//        for (NewsDocument news : unprocessedNews) {
//            try {
//
//                news.setProcessed(true);
//                newsRepository.save(news);
//            } catch (Exception e) {
//                log.error("뉴스 처리 실패: {}", news.getId(), e);
//            }
//        }
//    }
//
//
//}

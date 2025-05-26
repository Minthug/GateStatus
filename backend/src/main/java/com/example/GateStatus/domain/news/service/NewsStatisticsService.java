package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.dto.KeywordStats;
import com.example.GateStatus.domain.news.dto.TrendingKeyword;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsStatisticsService {

    private final NewsRepository newsRepository;
    private final MongoTemplate mongoTemplate;


    /**
     * 최근 일주일 간 키워드 빈도수 조회
     * 단순히 모든 키워드의 출현 횟수를 계산
     * @return
     */
    public Map<String, Long> getKeywordFrequency() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        List<NewsDocument> recentNews = newsRepository.findByPubDateAfter(weekAgo);

        log.info("최근 1주일 뉴스 {}건에서 키워드 빈도 분석", recentNews.size());


        return recentNews.stream()
                .filter(news -> news.getExtractedKeywords() != null)
                .flatMap(news -> news.getExtractedKeywords().stream())
                .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                .collect(Collectors.groupingBy(
                        keyword -> keyword,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(50)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 특정 기간의 상위 키워드 분석 (MongoDB Aggregation 사용)
     * 더 효율적이고 정확한 분석을 위해 Aggregation 사용
     * @param start
     * @param end
     * @return
     */
    public List<KeywordStats> analyzeTopKeywords(LocalDateTime start, LocalDateTime end, int limit) {
        log.info("키워드 분석: {} ~ {}, 상위 {}개", start, end, limit);

        Aggregation aggregation = Aggregation.newAggregation(
                // 1. 기간 필터링
                Aggregation.match(
                        Criteria.where("pubDate").gte(start).lt(end)
                                .and("Processed").is(true)
                ),
                // 2. 키워드 배열 풀기
                Aggregation.unwind("extractedKeywords"),
                // 3. 키워드별 그룹화 및 통계
                Aggregation.group("extractedKeywords")
                        .count().as("count")
                        .addToSet("category").as("categories")
                        .addToSet("source").as("sources"),
                // 4. 정렬
                Aggregation.sort(Sort.Direction.DESC, "count"),
                // 5. 상위 N개 선택
                Aggregation.limit(limit),
                // 6. 결과 형태 변환
                Aggregation.project()
                        .and("_id").as("keyword")
                        .and("count").as("frequency")
                        .and("categories").as("relatedCategories")
                        .and("source").as("newsSources")
                );
        AggregationResults<KeywordStats> results = mongoTemplate.aggregate(
                aggregation,
                NewsDocument.class,
                KeywordStats.class
        );
        return results.getMappedResults();
    }

    /**
     * 실시간 트렌드 키워드 계산
     * 시간 가중치와 인기도를 고려한 트렌드 점수 계산
     * @param hours
     * @return
     */
    public List<TrendingKeyword> calculateTrendingKeywords(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        log.info("트렌딩 키워드 계산: 최근 {}시간", hours);

        // 최근 뉴스 조회
        List<NewsDocument> recentNews = newsRepository.findByPubDateAfterOrderByPubDateDesc(cutoff);

        // 키워드별 트렌드 점수 계산
        Map<String, TrendScore> trendScores = new HashMap<>();

        for (NewsDocument news : recentNews) {
            if (news.getExtractedKeywords() == null) continue;

            double timeWeight = calculateTimeWeight(news.getPubDate(), hours);

            double popularityWeight = calculatePopularityWeight(news);

            for (String keyword : news.getExtractedKeywords()) {
                if (keyword == null || keyword.trim().isEmpty()) continue;

                trendScores.computeIfAbsent(keyword, k -> new TrendScore())
                        .addScore(timeWeight * popularityWeight)
                        .incrementCount()
                        .addNewsId(news.getId());
            }
        }

        return trendScores.entrySet().stream()
                .map(entry -> new TrendingKeyword(
                        entry.getKey(),
                        entry.getValue().getScore(),
                        entry.getValue().getCount(),
                        entry.getValue().getGrowthRate(hours),
                        entry.getValue().getRecentNewsIds()
                ))
                .sorted(Comparator.comparing(TrendingKeyword::score).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getHourlyDistribution(LocalDateTime date) {


    }


    private double calculatePopularityWeight(NewsDocument news) {
    }

    private double calculateTimeWeight(LocalDateTime pubDate, int hours) {
        return 0;
    }

    private static class TrendScore {
        private Double score = 0.0;
        private int count = 0;
        private List<String> recentNewsIds = new ArrayList<>();
        private LocalDateTime firstSeen = LocalDateTime.now();

        public TrendScore addScore(double points) {
            this.score += points;
            return this;
        }

        public TrendScore incrementCount() {
            this.count++;
            return this;
        }

        public TrendScore addNewsId(String newsId) {
            if (recentNewsIds.size() < 5) {
                recentNewsIds.add(newsId);
            }
            return this;
        }

        public double getScore() {
            return score;
        }

        public int getCount() {
            return count;
        }

        public List<String> getRecentNewsIds() {
            return recentNewsIds;
        }

        public double getGrowthRate(int hours) {
            long hoursElapsed = ChronoUnit.HOURS.between(firstSeen, LocalDateTime.now());
            if (hoursElapsed == 0) return score;
            return score / hoursElapsed;
        }
    }
}

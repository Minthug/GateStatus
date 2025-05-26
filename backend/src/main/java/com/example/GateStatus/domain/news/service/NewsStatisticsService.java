package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.dto.CategoryStats;
import com.example.GateStatus.domain.news.dto.FigureMentionStats;
import com.example.GateStatus.domain.news.dto.KeywordStats;
import com.example.GateStatus.domain.news.dto.TrendingKeyword;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 시간대별 뉴스 발행량 분포
     * 특정 날짜의 24시간 뉴스 발행 패턴 분석
     * @param date
     * @return
     */
    public Map<String, Integer> getHourlyDistribution(LocalDateTime date, Pageable pageable) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        log.info("시간대별 뉴스 분포 분석: {}", date.toLocalDate());

        // 해당 날짜의 모든 뉴스 조회
        List<NewsDocument> dailyNews = newsRepository.findByPubDateBetween(startOfDay, endOfDay);

        // 시간대별 집계
        Map<Integer, Integer> hourlyCount = new TreeMap<>();

        // 0 - 23 시 초기화
        for (int i = 0; i < 24; i++) {
            hourlyCount.put(i, 0);
        }

        // 뉴스 시간대별 분류
        for (NewsDocument news : dailyNews) {
            int hours = news.getPubDate().getHour();
            hourlyCount.merge(hours, 1, Integer::sum);
        }

        // 결과를 보기 좋게 변환
        Map<String, Integer> result = new LinkedHashMap<>();
        hourlyCount.forEach((hour, count) -> {
            String timeRange = String.format("%02d:00-%02d:59", hour, hour);
            result.put(timeRange, count);
        });

        // 통계 정보 로깅
        int totalNews = dailyNews.size();
        int peakHour = hourlyCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        log.info("일일 뉴스 총 {}건, 피크 시간대: {}시 ({}건)",
                totalNews, peakHour, hourlyCount.get(peakHour));

        return result;
    }

    /**
     * 카테고리별 뉴스 분포 통계
     * @param start
     * @param end
     * @return
     */
    public Map<String, CategoryStats> getCategoryDistribution(LocalDateTime start, LocalDateTime end) {
        List<NewsDocument> news = newsRepository.findByPubDateBetween(start, end);

        Map<String, CategoryStats> categoryStatsMap = new HashMap<>();

        for (NewsDocument doc : news) {
            String category = doc.getCategory() != null ? doc.getCategory() : "UNKNOWN";

            categoryStatsMap.computeIfAbsent(category, k -> new CategoryStats(k))
                    .addNews(doc);
        }
        return categoryStatsMap;
    }

    /**
     * 정치인별 언급 통계
     * @param start
     * @param end
     * @param limit
     * @return
     */
    public List<FigureMentionStats> getTopMentionedFigures(LocalDateTime start, LocalDateTime end, int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("pubDate").gte(start).lte(end)
                                .and("mentionedFiguresIds").exists(true).ne(null)
                ),
                Aggregation.unwind("mentionedFiguresIds"),
                Aggregation.group("mentionedFiguresIds")
                        .count().as("mentionCount")
                        .addToSet("category").as("categories"),
                Aggregation.sort(Sort.Direction.DESC, "mentionCount"),
                Aggregation.limit(limit),
                Aggregation.project()
                        .and("_id").as("figureId")
                        .and("mentionCount").as("count")
                        .and("categories").as("mentionedInCategories")
        );

        return mongoTemplate.aggregate(
                aggregation,
                NewsDocument.class,
                FigureMentionStats.class
        ).getMappedResults();
    }

    /**
     * 인기도 가중치 계산
     * @param news
     * @return
     */
    private double calculatePopularityWeight(NewsDocument news) {
        int viewCount = news.getViewCount() != null ? news.getViewCount() : 0;
        int commentCount = news.getCommentCount() != null ? news.getCommentCount() : 0;

        double viewScore = Math.log10(1 + viewCount);
        double commentScore = Math.log10(1 + commentCount) * 2; // 댓글에 더 높은 가중치

        return 1 + (viewScore + commentScore) / 10;
    }

    /**
     * 시간 가중치 계산 (최신일수록 높은 가중치)
     * @param pubDate
     * @return
     */
    private double calculateTimeWeight(LocalDateTime pubDate, int totalHours) {
        long hoursAgo = ChronoUnit.HOURS.between(pubDate, LocalDateTime.now());

        // 지수 감소 함수 사용 (e^(-x))
        double decayFactor = Math.exp(-hoursAgo / (double) totalHours);

        // 최소 가중치 0.1 보장
        return Math.max(decayFactor, 0.1);
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

package com.example.GateStatus.domain.news.repository;

import com.example.GateStatus.domain.news.NewsDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsRepository extends MongoRepository<NewsDocument, String> {

    // 처리되지 않은 뉴스 조회
    Page<NewsDocument> findByProcessedFalseOrderByPubDateDesc(Pageable pageable);

    // 특정 기간 내 뉴스 조회
    @Query("{'pubDate': {$gte: ?0, $lte: ?1}}")
    Page<NewsDocument> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // 특정 이슈와 연결된 뉴스 조회
    Page<NewsDocument> findByRelatedIssueIdOrderByPubDateDesc(String issueId, Pageable pageable);

    // 특정 정치인이 언급된 뉴스 조회
    @Query("{'mentionedFigureIds': ?0}")
    Page<NewsDocument> findByMentionedFigure(Long figureId, Pageable pageable);

    // 키워드로 뉴스 검색 (제목, 내용)
    @Query("{'$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}]}")
    Page<NewsDocument> searchByKeyword(String keyword, Pageable pageable);

    // 카테고리별 최신 뉴스 조회
    Page<NewsDocument> findByCategoryAndProcessedTrueOrderByPubDateDesc(String category, Pageable pageable);

    // 중요도가 높은 미처리 뉴스 조회 (조회수, 댓글수 기준)
    @Query("{'processed': false, '$or': [{'viewCount': {$gte: ?0}}, {'commentCount': {$gte: ?1}}]}")
    List<NewsDocument> findSignificantUnprocessedNews(Integer minViewCount, Integer minCommentCount);

    // 특정 소스의 최신 뉴스 조회
    Page<NewsDocument> findBySourceOrderByPubDateDesc(String source, Pageable pageable);

    // 중복 뉴스 확인 (제목 유사도)
    @Query("{'title': {$regex: ?0, $options: 'i'}, 'pubDate': {$gte: ?1}}")
    List<NewsDocument> findSimilarNewsByTitle(String titlePattern, LocalDateTime sinceDate);

    // 특정 기간 동안 가장 많이 언급된 정치인 ID 조회
    @Query(value = "{'pubDate': {$gte: ?0, $lte: ?1}}",
            fields = "{'mentionedFigureIds': 1}")
    List<NewsDocument> findMentionedFiguresInPeriod(LocalDateTime startDate, LocalDateTime endDate);

    // 처리 완료된 뉴스 중 이슈와 연결되지 않은 뉴스
    @Query("{'processed': true, 'relatedIssueId': null}")
    Page<NewsDocument> findProcessedButUnlinkedNews(Pageable pageable);

    // 특정 날짜 이후 발행된 뉴스 조회
    List<NewsDocument> findByPubDateAfter(LocalDateTime date);

    // 컨텐츠 해시와 생성일로 중복 체크
    List<NewsDocument> findByContentHashAndCreatedAtAfter(String contentHash, LocalDateTime createdAt);

    List<NewsDocument> findByProcessedFalseAndPubDateAfterOrderByViewCountDesc(LocalDateTime cutoff);

    // 오래된 뉴스 삭제용
    void deleteByCreatedAtBefore(LocalDateTime date);

    // 특정 키워드를 포함한 최근 N시간 내 뉴스 개수
    @Query(value = "{'extractedKeywords': ?0, 'pubDate': {$gte: ?1}}", count = true)
    long countRecentNewsByKeyword(String keyword, LocalDateTime since);

    Optional<NewsDocument> findByContentHash(String contentHash);

    Page<NewsDocument> findAllByOrderByPubDateDesc(Pageable pageable);

    Page<NewsDocument> findByCategoryOrderByPubDateDesc(String category, Pageable pageable);

    Page<NewsDocument> findByPubDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<NewsDocument> findByRelatedIssueIdIsNullOrderByPubDateDesc(Pageable pageable);

    long deleteByCreatedAtBeforeAndRelatedIssueIdIsNull(LocalDateTime cutoffDate);
}

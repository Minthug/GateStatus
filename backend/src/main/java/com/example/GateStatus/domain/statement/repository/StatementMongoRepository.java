package com.example.GateStatus.domain.statement.repository;

import com.example.GateStatus.domain.dashboard.dto.internal.CategoryCount;
import com.example.GateStatus.domain.dashboard.dto.internal.KeywordCount;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import org.apache.kafka.common.metrics.Stat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface StatementMongoRepository extends MongoRepository<StatementDocument, String> {

    Page<StatementDocument> findByFigureId(Long figureId, Pageable pageable);

    Page<StatementDocument> findByFigureName(String figureName, Pageable pageable);

    List<StatementDocument> findByType(StatementType type);

    @Query("{ 'statementDate': { $gte: ?0, $lte: ?1 } }")
    List<StatementDocument> findByPeriod(LocalDate startDate, LocalDate endDate);

    Page<StatementDocument> findAllByOrderByViewCountDesc(Pageable pageable);

    List<StatementDocument> findByFactCheckScoreGreaterThanEqual(Integer minScore);

    List<StatementDocument> findBySource(String source);

    boolean existsByOriginalUrl(String originalUrl);

    // 특정 정치인의 특정 기간 내 발언 조회
    @Query("{'figureId': ?0, 'statementDate': {$gte: ?1, $lte: ?2}}")
    List<StatementDocument> findByFigureIdAndStatementDateBetween(Long figureId, LocalDate startDate, LocalDate endDate, Pageable pageable);

//    // 특정 정치인의 특정 기간 내 모든 발언 조회(페이징없음)
//    List<StatementDocument> findByFigureIdAndStatementDateBetween(Long figureId, LocalDate startDate, LocalDate endDate);

    // 특정 정치인의 특정 이슈에 관한 특정 기간 내 발언 조회 (단일 정치인용)
    @Query("{'figureId': ?0, 'issueIds': ?1, 'statementDate': {$gte: ?2, $lte: ?3}}")
    List<StatementDocument> findByFigureIdAndIssueIdsContainingAndStatementDateBetween(
            Long figureId, String issueId, LocalDate startDate, LocalDate endDate);

    // 여러 정치인용 (배치 처리)
    @Query("{'figureId': {$in: ?0}, 'issueIds': ?1, 'statementDate': {$gte: ?2, $lte: ?3}}")
    List<StatementDocument> findByFiguresIdInAndIssueIdsContainingAndStatementDateBetween(
            List<Long> figureIds, String issueId, LocalDate startDate, LocalDate endDate);

    // 여러 정치인용 + 페이징 (전체 조회용)
    @Query("{'figureId': {$in: ?0}, 'statementDate': {$gte: ?1, $lte: ?2}}")
    List<StatementDocument> findByFigureIdInAndStatementDateBetween(
            List<Long> figureIds, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 발언 내용만 검색
    @Query("{ 'content': { $regex: ?0, $options:  'i' } }")
    List<StatementDocument> findByContentContainingKeyword(String keyword);



    // 정규식 기반 내용 검색 (제목과 내용에서 검색)
    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }")
    Page<StatementDocument> searchByRegex(String keyword, Pageable pageable);

    // 텍스트 인덱스 기반 검색 (인덱스 설정 필요)
    @Query("{'$text': {$search: ?0}}")
    Page<StatementDocument> fullTextSearch(String keyword, Pageable pageable);

    // 발언 내용에서 정확한 문구 검색 (정확한 일치)
    @Query("{'content': {$regex: ?0}}")
    List<StatementDocument> findByExactPhraseInContent(String phrase);

    // 여러 키워드를 모두 포함하는 발언 검색(AND 조건)
    @Query("{ $and: [ {'content': {$regex: ?0, $options: 'i'}}, {'content': {$regex: ?1, $options: 'i'}} ] }")
    List<StatementDocument> findByMultipleKeywords(String keyword1, String keyword2);


    // 발언 내용 일부로 검색  (더 유연한 검색을 위한 메서드)
    @Query("{'content': {$regex: '.*?0.*', $options: 'i'}}")
    Page<StatementDocument> findByContentContaining(String contentSnippet, Pageable pageable);

    @Query("{'content': {$regex: ?0, $options: 'i'}}")
    List<StatementDocument> findByKeywordOrderByStatementDateDesc(String keyword, Pageable pageable);

    @Query("{'$expr': {$gt: [{$strLenCp: '$content'}, ?0]}}")
    List<StatementDocument> findByContentLengthGreaterThan(int length, Pageable pageable);

    @Query("{'$expr': {$lt: [{$strLenCp: '$content'}, ?0]}}")
    List<StatementDocument> findByContentLengthLessThan(int length, Pageable pageable);


    long countByFigureId(Long figureId);

    @Aggregation(pipeline = {
            "{ $match: { figureId: ?0 } }",
            "{ $group: { _id: '$cagegoryName', count: { $sum: 1 } } }",
            "{ $sort: { count: -1 } }"
    })
    List<CategoryCount> countByCategory(Long figureId);

    @Aggregation(pipeline = {
            "{ $match: { figureId: ?0 } }",
            "{ $project: { words: { $split: [\"$content\", \" \"] } } }",
            "{ $unwind: \"$words\" }",
            "{ $match: { words: { $not: { $regex: '^[0-9]*$' }, $nin: ?1 } } }", // 수정: $not 연산자 형식 변경
            "{ $group: { _id: \"$words\", count: { $sum: 1 } } }",
            "{ $match: { count: { $gt: 1 } } }", // 2회 이상 등장한 키워드만
            "{ $sort: { count: -1 } }",
            "{ $limit: 50 }"
    })
    List<KeywordCount> findTopKeywords(Long figureId, List<String> stopwords);


    // 배치 처리용 메서드 추가
    @Query("{'statementDate': {$gte: ?0, $lte: ?1}}")
    List<StatementDocument> findByStatementDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("{'figureId': ?0, 'statementDate': {$gte: ?1, $lte: ?2}}")
    long countByFigureIdAndDateRange(Long figureId, LocalDate startDate, LocalDate endDate);


    @Aggregation(pipeline = {
            "{ '$match': { 'figureId': ?0, 'statementDate': { '$gte': ?1, '$lte': ?2 } } }",
            "{ '$group': { '_id': '$category', 'count': { '$sum': 1 } } }",
            "{ '$sort': { 'count': -1 } }",
            "{ '$limit': 10 }"
    })
    List<CategoryCount> countByCategoryAndDateRange(Long figureId, LocalDate startDate, LocalDate endDate);

    @Aggregation(pipeline = {
            "{ '$match': { 'figureId': ?0, 'statementDate': { '$gte': ?2, '$lte': ?3 } } }",
            "{ '$project': { 'words': { '$split': ['$content', ' '] } } }",
            "{ '$unwind': '$words' }",
            "{ '$match': { 'words': { '$nin': ?1, '$regex': '^[가-힣]{2,}$' } } }",
            "{ '$group': { '_id': '$words', 'count': { '$sum': 1 } } }",
            "{ '$sort': { 'count': -1 } }",
            "{ '$limit': 20 }"
    })
    List<KeywordCount> findTopKeywordsByDateRange(Long figureId, List<String> stopwords, LocalDate startDate, LocalDate endDate);

}

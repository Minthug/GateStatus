package com.example.GateStatus.domain.statement.repository;

import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface StatementMongoRepository extends MongoRepository<StatementDocument, String> {

    Page<StatementDocument> findByFigureId(Long figureId, Pageable pageable);

    List<StatementDocument> findByType(StatementType type);

    // 텍스트 인덱스 기반 검색 (인덱스 설정 필요)
    @Query("{'$text': {$search: ?0}}")
    Page<StatementDocument> fullTextSearch(String keyword, Pageable pageable);

    @Query("{ 'statementDate': { $gte: ?0, $lte: ?1 } }")
    List<StatementDocument> findByPeriod(LocalDate startDate, LocalDate endDate);

    Page<StatementDocument> findAllByOrderByViewCountDesc(Pageable pageable);

    List<StatementDocument> findByFactCheckScoreGreaterThanEqual(Integer minScore);

    List<StatementDocument> findBySource(String source);

    // 정규식 기반 검색 (대안)
    @Query("{ 'content': { $regex: ?0, $options:  'i' } }")
    List<StatementDocument> findByContentContainingKeyword(String keyword);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }")
    Page<StatementDocument> searchByRegex(String keyword, Pageable pageable);

    boolean existsByOriginalUrl(String originalUrl);

    // 특정 정치인의 특정 기간 내 발언 조회
    List<StatementDocument> findByFigureIdAndStatementDateBetween(Long figureId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 특정 정치인의 특정 기간 내 모든 발언 조회(페이징없음)
    List<StatementDocument> findByFigureIdAndStatementDateBetween(Long figureId, LocalDate startDate, LocalDate endDate);

    // 특정 정치인의 특정 이슈에 관한 특정 기간 내 발언 조회
    @Query("{'figureId': ?0, 'issueIds': ?1, 'statementDate': {$gte: ?2, $lte: ?3}}")
    List<StatementDocument> findByFigureIdAndIssueIdsContainingAndStatementDateBetween(
            Long figureId, String issueId, LocalDate startDate, LocalDate endDate);

    Page<StatementDocument> findByFigureName(String figureName, Pageable pageable);

}

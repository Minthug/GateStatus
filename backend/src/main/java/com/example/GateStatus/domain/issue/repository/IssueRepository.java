package com.example.GateStatus.domain.issue.repository;

import com.example.GateStatus.domain.issue.IssueDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends MongoRepository<IssueDocument, String> {
    // 이름 기반 조회
    Optional<IssueDocument> findByNameAndIsActiveTrue(String name);
    boolean existsByNameAndIsActiveTrue(String name);

    // 검색 관련
    @Query("{'name': {$regex: '^?0$', $options: 'i'}, 'isActive': true}")
    Page<IssueDocument> findByNameIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

    @Query("{'name': {$regex: ?0, $options: 'i'}, 'isActive': true}")
    Page<IssueDocument> findByNameContainingAndIsActiveTrueOrderByViewCountDesc(String name, Pageable pageable);

    // 기존 메서드들...
    Page<IssueDocument> findByCategoryCodeAndIsActiveTrue(String categoryCode, Pageable pageable);
    Page<IssueDocument> findByIsHotTrueAndIsActiveTrueOrderByPriorityDescViewCountDesc(Pageable pageable);
    Page<IssueDocument> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    @Query("{'$text': {'$search': ?0}, 'isActive': true}")
    Page<IssueDocument> searchByKeyword(String keyword, Pageable pageable);

    // 관련 리소스 조회
    @Query("{'relatedFigureIds': ?0, 'isActive': true}")
    Page<IssueDocument> findByRelatedFigureIdsContaining(Long figureId, Pageable pageable);
    @Query("{'relatedBillIds': ?0, 'isActive': true}")
    List<IssueDocument> findByRelatedBillIdsContaining(String billId);
    List<IssueDocument> findByRelatedStatementIdsContaining(String statementId);
    List<IssueDocument> findByRelatedNewsIdsContaining(String newsId);

    // 관련 이슈 찾기
    @Query("{'categoryCode': ?0, 'id': {'$ne': ?1}, 'isActive': true}")
    List<IssueDocument> findRelatedIssuesByCategoryAndNotId(String categoryCode, String currentIssueId, Pageable pageable);
    @Query("{'$or': [{'keywords': {'$in': ?0}}, {'tags': {'$in': ?0}}], 'id': {'$ne': ?1}, 'isActive': true}")
    List<IssueDocument> findRelatedIssuesByKeywordsOrTags(List<String> keywords, String currentIssueId, Pageable pageable);



}


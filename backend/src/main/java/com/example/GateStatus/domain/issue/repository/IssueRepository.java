package com.example.GateStatus.domain.issue.repository;

import com.example.GateStatus.domain.issue.IssueDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends MongoRepository<IssueDocument, String> {

    Page<IssueDocument> findByCategoryCodeAndIsActiveTrue(String categoryCode, Pageable pageable);

    List<IssueDocument> findByCategoryCodeAndIsActiveTrue(String categoryCode);

    List<IssueDocument> findByCategoryCodeInAndIsActiveTrue(List<String> categoryCode);

    Page<IssueDocument> findByIsHotTrueAndIsActiveTrueOrderByPriorityDescViewCountDesc(Pageable pageable);

    @Query("{'relatedFigureIds': ?0, 'isActive': true}")
    Page<IssueDocument> findIssueByFigureId(Long figureId, Pageable pageable);

    Page<IssueDocument> findByParentIssueIdAndIsActiveTrue(String parentIssueId, Pageable pageable);

    Page<IssueDocument> findByTagsContainingAndIsActiveTrue(String tag, Pageable pageable);

    @Query("{'$text': {'$search': ?0}, 'isActive': true}")
    Page<IssueDocument> searchByKeyword(String keyword, Pageable pageable);

    @Query("{'relatedBillIds': ?0, 'isActive': true}")
    List<IssueDocument> findIssuesByBillId(String billId);

    List<IssueDocument> findByRelatedStatementIdsContaining(String statementId);

    Page<IssueDocument> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Optional<IssueDocument> findByIdAndIsActiveTrue(String id);

    @Query("{'categoryCode': ?0, 'id': {'$ne': ?1}, 'isActive': true}")
    List<IssueDocument> findRelatedIssuesByCategoryAndNotId(String categoryCode, String currentIssueId, Pageable pageable);

    // 쿼리 관련 및 ID 문제 변수명 문제 질문
    @Query("{'$or': [{'keywords': {'$in': ?0}}, {'tags': {'$in': ?0}}], 'id': {'$ne': ?1}, 'isActive': true}")
    List<IssueDocument> findRelatedIssuesByKeywordsOrTags(List<String> keywords, String currentIssueId, Pageable pageable);
}


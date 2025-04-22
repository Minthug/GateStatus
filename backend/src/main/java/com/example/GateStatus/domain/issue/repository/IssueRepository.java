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

    Page<IssueDocument> findByIsHotTrueAndIsActiveTrueOrderByPriorityDescViewCountDesc(Pageable pageable);

    @Query("{'relatedFigureIds': ?0, 'isActive': true}")
    Page<IssueDocument> findIssueByFigureId(Long figureId, Pageable pageable);

    Page<IssueDocument> findByParentIssueIdAndIsActiveTrue(String parentIssueId, Pageable pageable);

    Page<IssueDocument> findByTagsContainingAndIsActiveTrue(String tag, Pageable pageable);

    Page<IssueDocument> searchByKeyword(String keyword, Pageable pageable);

    List<IssueDocument> findIssuesByBillId(String billId);

    List<IssueDocument> findIssuesByStatementId(String statementId);

    Page<IssueDocument> findByIsActiveTrueAndOrderByCreatedAtDesc(Pageable pageable);

    Optional<IssueDocument> findByIdAndIsActiveTrue(String id);

    List<IssueDocument> findRelatedIssuesByCategoryAndNotId(String categoryCode, String currentIssueId, Pageable pageable);

    List<IssueDocument> findRelatedIssuesByKeywordsOrTags(List<String> keywords, String currentIssueId, Pageable pageable);


}

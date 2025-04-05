package com.example.GateStatus.domain.issue.repository;

import com.example.GateStatus.domain.issue.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    Page<Issue> findAllByOrderByCreatedDateDesc(Pageable pageable);

    List<Issue> findByFigureIdOrderByCreatedDateDesc(Long figureId);

    Page<Issue> findByFigureIdOrderByCreatedDateDesc(Long figureId, Pageable pageable);

    List<Issue> findByIsHotOrderByViewCountDesc(boolean isHot, Pageable pageable);

    Page<Issue> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword, Pageable pageable);

    @Query("SELECT i FROM Issue i JOIN i.tags t WHERE t = :tag")
    List<Issue> findByTag(@Param("tag") String tag);

    @Modifying
    @Transactional
    @Query("UPDATE Issue i SET i.viewCount = :viewCount + 1 WHERE i.id = :issueId")
    void incrementViewCount(@Param("issueId") Long issueId);

    @Modifying
    @Transactional
    @Query("UPDATE Issue i SET i.isHot = :isHot WHERE i.id = :issueId")
    void updateHotStatus(@Param("issueId") Long issueId, @Param("isHot") boolean isHot);

    List<Issue> findByFigureIdAndTagsContaining(Long figureId, String tag);

    @Query("SELECT i FROM Issue i WHERE i.createdDate >= :date")
    List<Issue> findRecentIssues(@Param("date") LocalDateTime date);

}

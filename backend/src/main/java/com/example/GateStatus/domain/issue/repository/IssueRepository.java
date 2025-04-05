package com.example.GateStatus.domain.issue.repository;

import com.example.GateStatus.domain.issue.Issue;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findAllByOrderByCreatedAtDesc(Pageable pageable);


    List<Issue> findByFigureIdOrderByCreatedDateDesc(Long figureId);

    List<Issue> findByIsHotOrderByViewCountDesc(boolean b, PageRequest of);


}

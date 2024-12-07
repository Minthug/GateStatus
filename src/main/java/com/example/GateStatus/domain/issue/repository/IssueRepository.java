package com.example.GateStatus.domain.issue.repository;

import com.example.GateStatus.domain.issue.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, Long> {

}

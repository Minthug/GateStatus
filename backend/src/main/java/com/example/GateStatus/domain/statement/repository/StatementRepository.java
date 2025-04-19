package com.example.GateStatus.domain.statement.repository;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.entity.StatementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    Page<Statement> findByFigure(Figure figure, Pageable pageable);

    List<Statement> findByType(StatementType type);

    Page<Statement> findByContentContainingOrTitleContaining(String contentKeyword, String titleKeyword, Pageable pageable);

    @Query("SELECT s FROM Statement s WHERE s.statementDate BETWEEN :startDate AND :endDate")
    List<Statement> findByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Page<Statement> findTopByOrderByViewCountDesc(Pageable pageable);

    @Query("SELECT s FROM Statement s WHERE s.factCheckScore >= :minScore")
    List<Statement> findByFactCheckScoreGreaterThanEqual(@Param("minScore") Integer minScore);

    List<Statement> findBySource(String source);
}

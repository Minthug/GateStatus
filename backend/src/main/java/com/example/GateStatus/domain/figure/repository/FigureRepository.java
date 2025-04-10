package com.example.GateStatus.domain.figure.repository;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FigureRepository extends JpaRepository<Figure, Long> {

    Optional<Figure> findByName(String name);

    Page<Figure> findByNameContaining(String keyword, Pageable pageable);

    Page<Figure> findByConstituencyContaining(String keyword, Pageable pageable);

    List<Figure> findByFigureType(FigureType figureType);

    List<Figure> findByFigureParty(FigureParty figureParty);

    List<Figure> findByNameContainingAndFigureParty(String name, FigureParty figureParty);

    @Query("SELECT f FROM Figure f ORDER BY f.viewCount DESC ")
    List<Figure> findTopByOrderByViewCountDesc(PageRequest pageRequest);

    @Modifying
    @Transactional
    @Query("UPDATE Figure f SET f.viewCount = :viewCount WHERE f.id = :figureId")
    void updateViewCount(@Param("figureId") Long figureId, @Param("viewCount") Long viewCount);

    @Query("SELECT f FROM Figure f JOIN f.figureTag ft JOIN ft.tag t WHERE t.tagName = :tagName")
    List<Figure> findByTagName(@Param("tagName") String tagName);

    List<Figure> findTop10ByOrderByModifiedDateDesc();
}

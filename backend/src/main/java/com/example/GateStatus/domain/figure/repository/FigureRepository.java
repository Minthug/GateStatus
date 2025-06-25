package com.example.GateStatus.domain.figure.repository;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FigureRepository extends JpaRepository<Figure, Long> {

    Optional<Figure> findByName(String name);

    @Query("SELECT f FROM Figure f WHERE f.figureId = :figureId")
    Optional<Figure> findByFigureId(@Param("figureId") String figureId);

    List<Figure> findAllByFigureIdIsNotNull();

    Page<Figure> findByNameContaining(String name, Pageable pageable);

    List<Figure> findByNameContaining(String name);

    Page<Figure> findByConstituencyContaining(String place, Pageable pageable);

    List<Figure> findByFigureType(FigureType figureType);

    Page<Figure> findByFigureParty(FigureParty figureParty, Pageable pageable);
    List<Figure> findByFigureParty(FigureParty figureParty);

    List<Figure> findByNameContainingAndFigureParty(String name, FigureParty figureParty);

    @Query("SELECT f FROM Figure f ORDER BY f.viewCount DESC ")
    List<Figure> findTopByOrderByViewCountDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Figure f SET f.viewCount = :viewCount WHERE f.id = :figureId")
    void updateViewCount(@Param("figureId") Long figureId, @Param("viewCount") Long viewCount);

    @Query("SELECT f FROM Figure f JOIN f.figureTag ft JOIN ft.tag t WHERE t.tagName = :tagName")
    List<Figure> findByTagName(@Param("tagName") String tagName);

    List<Figure> findTop10ByOrderByModifiedDateDesc(Pageable pageable);

    // 기본 정보만 로딩 (컬렉션 없이)
    @Query("SELECT f FROM Figure f WHERE f.figureId = :figureId")
    Optional<Figure> findByFigureIdWithoutCollections(@Param("figureId") String figureId);

    // 교육 정보만 함께 로딩
    @Query("SELECT f FROM Figure f LEFT JOIN FETCH f.education WHERE f.figureId = :figureId")
    Optional<Figure> findByFigureIdWithEducation(@Param("figureId") String figureId);

    // 경력 정보만 함께 로딩
    @Query("SELECT f FROM Figure f LEFT JOIN FETCH f.careers WHERE f.figureId = :figureId")
    Optional<Figure> findByFigureIdWithCareers(@Param("figureId") String figureId);

    // 사이트 정보만 함께 로딩
    @Query("SELECT f FROM Figure f LEFT JOIN FETCH f.sites WHERE f.figureId = :figureId")
    Optional<Figure> findByFigureIdWithSites(@Param("figureId") String figureId);

    // 활동 정보만 함께 로딩
    @Query("SELECT f FROM Figure f LEFT JOIN FETCH f.activities WHERE f.figureId = :figureId")
    Optional<Figure> findByFigureIdWithActivities(@Param("figureId") String figureId);

    boolean existsByFigureId(String figureId);


    List<Figure> findByNameIn(List<String> cleanNames);

    void incrementViewCount(String figureId);

    Page<Figure> findByNameContainingOrConstituencyContaining(String keyword, String keyword2, Pageable pageable);
    List<Figure> findByNameContainingOrConstituencyContaining(String keyword, String keyword2);
}

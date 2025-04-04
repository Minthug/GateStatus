package com.example.GateStatus.domain.figure.repository;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FigureRepository extends JpaRepository<Figure, Long> {

    Optional<Figure> findByName(String name);


    Page<Figure> findByNameContaining(String keyword, Pageable pageable);

    Page<Figure> findByPlaceContaining(String keyword, Pageable pageable);

    List<Figure> findByFigureType(FigureType figureType);

    List<Figure> findTopByOrderByViewCountDesc(PageRequest pageRequest);

    void updateViewCount(Long figureId, Long viewCount);
}

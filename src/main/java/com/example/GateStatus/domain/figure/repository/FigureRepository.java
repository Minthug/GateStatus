package com.example.GateStatus.domain.figure.repository;

import com.example.GateStatus.domain.figure.Figure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FigureRepository extends JpaRepository<Figure, Long> {

    Optional<Figure> findByName(String name);
}

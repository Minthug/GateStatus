package com.example.GateStatus.domain.tag;

import com.example.GateStatus.domain.figure.Figure;
import jakarta.persistence.*;

@Entity
public class FigureTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Figure figure;
    private String tagName;
}

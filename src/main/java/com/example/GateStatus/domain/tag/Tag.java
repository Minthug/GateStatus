package com.example.GateStatus.domain.tag;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "tag")
    private List<FigureTag> figureTags;

    private String tagName;
}

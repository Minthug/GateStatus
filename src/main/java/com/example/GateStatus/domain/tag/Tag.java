package com.example.GateStatus.domain.tag;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureTag;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "tag")
    private List<FigureTag> figureTags;

    private String tagName;
}

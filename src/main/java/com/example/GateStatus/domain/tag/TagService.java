package com.example.GateStatus.domain.tag;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final FigureRepository figureRepository;

    @Transactional
    public Tag
}

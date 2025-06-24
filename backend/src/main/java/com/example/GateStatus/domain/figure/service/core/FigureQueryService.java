package com.example.GateStatus.domain.figure.service.core;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.figure.service.request.FigureSearchRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public class FigureQueryService {
    public Figure findByFigureId(String figureId) {
        return null;
    }

    public void incrementViewCountAsync(String figureId) {

    }

    public Page<Figure> findAllWithCriteria(FigureSearchRequest request) {


    }

    public List<Figure> findPopularFigures(int limit) {
        return null;
    }

    public List<Figure> findByParty(FigureParty party) {
        return null;
    }

    public boolean existsByFigureId(String figureId) {
        return false;
    }
}

package com.example.GateStatus.domain.figure.service.request;

import com.example.GateStatus.domain.figure.FigureParty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FigureSearchRequest {
    private String keyword;
    private String searchType = "all";
    private FigureParty party;
    private int page = 0;
    private int size = 20;
    private String sortBy = "name";
    private String sortDirection = "ASC";

    public static FigureSearchRequest of(String keyword) {
        return FigureSearchRequest.builder()
                .keyword(keyword)
                .build();
    }
}

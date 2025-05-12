package com.example.GateStatus.domain.statement.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FactCheckRequest(@NotNull(message = "팩트체크 점수는 필수입니다") @Min(value = 0, message = "팩트체크 점수는 0점 이상이어야 합니다")
                               @Max(value = 100, message = "팩트체크 점수는 100점 이하이어야 합니다") Integer score,
                               @NotBlank(message = "팩트체크 결과는 필수입니다")
                               String result,
                               String evidenceUrl,
                               String checkerName,
                               String checkerInstitution,
                               String analysisDetail,
                               List<String> checkableItems) {

    public static FactCheckRequest of(Integer score, String result) {
        return new FactCheckRequest(score, result, null, null, null, null, null);
    }

    public static FactCheckRequest full(Integer score, String result, String evidenceUrl,
                                        String checkerName, String checkerInstitution,
                                        String analysisDetail, List<String> checkableItems) {
        return new FactCheckRequest(score, result, evidenceUrl, checkerName, checkerInstitution, analysisDetail, checkableItems);

    }
}

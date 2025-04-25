package com.example.GateStatus.domain.issue;

import com.example.GateStatus.domain.issue.exception.InvalidCategoryException;
import org.springframework.stereotype.Component;

@Component
public class IssueCategoryValidator {

    public IssueCategory validateAndCategory(String code) {
        try {
            return IssueCategory.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidCategoryException("유효하지 않은 카테고리 코드: " + code);
        }
    }
}

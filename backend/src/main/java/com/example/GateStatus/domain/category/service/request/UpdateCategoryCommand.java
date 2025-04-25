package com.example.GateStatus.domain.category.service.request;

import com.example.GateStatus.domain.category.CategorySubject;
import com.example.GateStatus.domain.category.CategoryType;

public record UpdateCategoryCommand(String name, String description, String iconUrl,
                                    Integer displayOrder, Boolean active, CategoryType type, CategorySubject subject) {
}

package com.example.GateStatus.domain.category.service.response;

import com.example.GateStatus.domain.category.Category;
import com.example.GateStatus.domain.category.CategorySubject;
import com.example.GateStatus.domain.category.CategoryType;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

public record CategoryResponse(Long id, String name, String description, String iconUrl,
                               int displayOrder, boolean active, CategoryType type, CategorySubject subject) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIconUrl(),
                category.getDisplayOrder(),
                category.isActive(),
                category.getType(),
                category.getSubject()
        );
    }
}

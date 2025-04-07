package com.example.GateStatus.domain.category.service.request;

import com.example.GateStatus.domain.category.CategoryType;

public record CreateCategoryCommand(String name, String description, String iconUrl,
                                    int displayOrder, boolean active, CategoryType type) {
}

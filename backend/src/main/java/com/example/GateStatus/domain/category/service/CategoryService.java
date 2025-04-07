package com.example.GateStatus.domain.category.service;

import com.example.GateStatus.domain.category.Category;
import com.example.GateStatus.domain.category.repository.CategoryRepository;
import com.example.GateStatus.domain.category.service.request.CreateCategoryCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Long createCategory(CreateCategoryCommand command) {
        Category category = Category.builder()
                .name(command.name())
                .description(command.description())
                .iconUrl(command.iconUrl())
                .displayOrder(command.displayOrder())
                .active(command.active())
                .type(command.type())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return savedCategory.getId();
    }
}

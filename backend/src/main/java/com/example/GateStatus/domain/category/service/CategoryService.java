package com.example.GateStatus.domain.category.service;

import com.example.GateStatus.domain.category.Category;
import com.example.GateStatus.domain.category.CategoryType;
import com.example.GateStatus.domain.category.repository.CategoryRepository;
import com.example.GateStatus.domain.category.service.request.CreateCategoryCommand;
import com.example.GateStatus.domain.category.service.request.UpdateCategoryCommand;
import com.example.GateStatus.domain.category.service.response.CategoryResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveIsTrueOrderByDisplayOrderAsc()
                .stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getCategoriesByType(CategoryType type) {
        return categoryRepository.findByTypeOrderByDisplayOrderAsc(type)
                .stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryCommand command) {
        Category category = Category.builder()
                .name(command.name())
                .description(command.description())
                .iconUrl(command.iconUrl())
                .displayOrder(command.displayOrder())
                .active(command.active())
                .type(command.type())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.from(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryCommand command) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다: " + id));

        category.update(
                command.name(),
                command.description(),
                command.iconUrl(),
                command.displayOrder(),
                command.active(),
                command.type()
        );

        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }
}

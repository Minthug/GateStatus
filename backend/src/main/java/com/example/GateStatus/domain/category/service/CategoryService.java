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

    @Transactional(readOnly = true)
    public CategoryResponse findCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다"));

        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateCategory(Long categoryId, UpdateCategoryCommand command) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다"));

        category.update(
                command.name(),
                command.description(),
                command.iconUrl(),
                command.displayOrder(),
                command.active(),
                command.type()
        );
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findCategoriesByType(CategoryType type) {
        return categoryRepository.findByType(type).stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }
}

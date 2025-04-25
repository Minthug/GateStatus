package com.example.GateStatus.domain.category.controller;

import com.example.GateStatus.domain.category.CategoryType;
import com.example.GateStatus.domain.category.service.CategoryService;
import com.example.GateStatus.domain.category.service.request.CreateCategoryCommand;
import com.example.GateStatus.domain.category.service.request.UpdateCategoryCommand;
import com.example.GateStatus.domain.category.service.response.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllActiveCategories());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByType(@PathVariable CategoryType type) {
        return ResponseEntity.ok(categoryService.getCategoriesByType(type));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CreateCategoryCommand command) {
        CategoryResponse response = categoryService.createCategory(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @RequestBody UpdateCategoryCommand command) {
        CategoryResponse response = categoryService.updateCategory(id, command);

        return ResponseEntity.ok(response);
    }
}

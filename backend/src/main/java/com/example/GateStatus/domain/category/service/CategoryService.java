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


}

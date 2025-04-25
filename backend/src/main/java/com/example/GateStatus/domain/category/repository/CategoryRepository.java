package com.example.GateStatus.domain.category.repository;

import com.example.GateStatus.domain.category.Category;
import com.example.GateStatus.domain.category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByTypeOrderByDisplayOrderAsc(CategoryType type);
    List<Category> findByActiveIsTrueOrderByDisplayOrderAsc();
    Optional<Category> findByName(String name);

}

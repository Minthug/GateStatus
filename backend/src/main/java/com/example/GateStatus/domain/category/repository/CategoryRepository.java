package com.example.GateStatus.domain.category.repository;

import com.example.GateStatus.domain.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}

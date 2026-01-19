package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByUserIdAndNameAndIsActiveTrue(Long userId, String name);

    Optional<Category> findByIdAndUserId(Long categoryId, Long userId);

    // 목록 조회
    List<Category> findAllByUserIdAndIsActiveTrueOrderByIdAsc(Long userId);
}
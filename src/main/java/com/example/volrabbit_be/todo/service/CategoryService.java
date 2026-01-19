package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.todo.domain.Category;
import com.example.volrabbit_be.todo.dto.CategoryDto;
import com.example.volrabbit_be.todo.repository.CategoryRepository;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CategoryDto.CreateResponse create(Long userId, CategoryDto.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        if (categoryRepository.existsByUserIdAndNameAndIsActiveTrue(userId, request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 카테고리 이름입니다.");
        }

        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .build();

        Category saved = categoryRepository.save(category);

        return CategoryDto.CreateResponse.builder()
                .categoryId(saved.getId())
                .build();
    }

    @Transactional
    public CategoryDto.DeleteResponse delete(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));

        if (!category.isActive()) {
            throw new IllegalArgumentException("이미 삭제(비활성화)된 카테고리입니다.");
        }

        category.deactivate();

        return CategoryDto.DeleteResponse.builder()
                .categoryId(category.getId())
                .active(category.isActive()) // false
                .build();
    }

    // 카테고리 목록 조회(isActive=true)
    public CategoryDto.ListResponse getAll(Long userId) {
        List<CategoryDto.CategoryResponse> categories = categoryRepository
                .findAllByUserIdAndIsActiveTrueOrderByIdAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return CategoryDto.ListResponse.builder()
                .categories(categories)
                .build();
    }

    // 카테고리 단건 조회(isActive=true, 활성화된 카테고리만 조회 가능)
    public CategoryDto.CategoryResponse getOne(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserIdAndIsActiveTrue(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));

        return toResponse(category);
    }

    private CategoryDto.CategoryResponse toResponse(Category category) {
        return CategoryDto.CategoryResponse.builder()
                .categoryId(category.getId())
                .name(category.getName())
                .active(category.isActive())
                .build();
    }
}

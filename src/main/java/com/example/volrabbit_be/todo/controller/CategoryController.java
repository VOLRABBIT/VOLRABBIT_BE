package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.todo.dto.CategoryDto;
import com.example.volrabbit_be.todo.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDto.CreateResponse> create(
            Authentication authentication,
            @RequestBody CategoryDto.CreateRequest request
    ) {
        Long userId = extractUserId(authentication);
        CategoryDto.CreateResponse response = categoryService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 목록 조회
    @GetMapping
    public ResponseEntity<CategoryDto.ListResponse> getAll(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(categoryService.getAll(userId));
    }

    // 단건 조회
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto.CategoryResponse> getOne(
            Authentication authentication,
            @PathVariable Long categoryId
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(categoryService.getOne(userId, categoryId));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<CategoryDto.DeleteResponse> delete(
            Authentication authentication,
            @PathVariable Long categoryId
    ) {
        Long userId = extractUserId(authentication);
        CategoryDto.DeleteResponse response = categoryService.delete(userId, categoryId);
        return ResponseEntity.ok(response);
    }

    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return Long.parseLong(userDetails.getUsername()); // JWT(userId)
        }
        if (principal instanceof String s) {
            return Long.parseLong(s);
        }
        throw new IllegalArgumentException("인증 사용자 정보를 가져올 수 없습니다.");
    }
}
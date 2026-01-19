package com.example.volrabbit_be.todo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class CategoryDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String name;
    }

    @Getter
    @Builder
    public static class CreateResponse {
        private Long categoryId;
    }

    @Getter
    @Builder
    public static class DeleteResponse {
        private Long categoryId;
        private boolean active; // 삭제 후 false
    }

    // 조회
    @Getter
    @Builder
    public static class CategoryResponse {
        private Long categoryId;
        private String name;
        private boolean active;
    }

    @Getter
    @Builder
    public static class ListResponse {
        private List<CategoryResponse> categories;
    }
}
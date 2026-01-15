package com.example.volrabbit_be.global.common;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final String status;
    private final String message;
    private final T data;

    public ApiResponse(T data) {
        this.status = "SUCCESS";
        this.message = "요청이 성공적으로 처리되었습니다.";
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }
}
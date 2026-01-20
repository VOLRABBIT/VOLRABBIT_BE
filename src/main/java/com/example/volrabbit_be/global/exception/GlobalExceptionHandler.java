package com.example.volrabbit_be.global.exception;

import com.example.volrabbit_be.global.common.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleException(IllegalArgumentException e) {
        return ApiResponse.error(e.getMessage());
    }
}

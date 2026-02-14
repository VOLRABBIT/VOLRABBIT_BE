package com.example.volrabbit_be.global.exception;

import com.example.volrabbit_be.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleException(IllegalArgumentException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ApiExceptions.Conflict.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(ApiExceptions.Conflict e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ApiExceptions.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ApiExceptions.NotFound e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ApiExceptions.BadRequest.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(ApiExceptions.BadRequest e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ApiExceptions.Unauthorized.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorized(ApiExceptions.Unauthorized e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return ApiResponse.error(message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(HandlerMethodValidationException e) {
        // 반환값에서 검증 오류가 발생한 경우
        if (e.isForReturnValue()) {
            return ResponseEntity.internalServerError().body(handleAll(e));
        }

        // 요청과 관련한 검증 오류가 발생한 경우
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleAll(Exception e) {
        return ApiResponse.error("서버 오류가 발생했습니다.");
    }
}

package com.example.volrabbit_be.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public final class ApiExceptions {

    private ApiExceptions() {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class NotFound extends RuntimeException {
        public NotFound(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class Conflict extends RuntimeException {
        public Conflict(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class BadRequest extends RuntimeException {
        public BadRequest(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class Unauthorized extends RuntimeException {
        public Unauthorized(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class Forbidden extends RuntimeException {
        public Forbidden(String message) {
            super(message);
        }
    }
}

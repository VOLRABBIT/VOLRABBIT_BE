package com.example.volrabbit_be.todo.domain.enumType;

public enum PriorityQuadrant {
    URGENT_IMPORTANT,
    URGENT_NOT_IMPORTANT,
    NOT_URGENT_IMPORTANT,
    NOT_URGENT_NOT_IMPORTANT;

    public String label() {
        return switch (this) {
            case URGENT_IMPORTANT -> "긴급·중요";
            case URGENT_NOT_IMPORTANT -> "긴급";
            case NOT_URGENT_IMPORTANT -> "중요";
            case NOT_URGENT_NOT_IMPORTANT -> "일반"; // 또는 "여유"
        };
    }
}

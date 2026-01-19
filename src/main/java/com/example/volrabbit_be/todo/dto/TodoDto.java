package com.example.volrabbit_be.todo.dto;

import com.example.volrabbit_be.todo.domain.enumType.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TodoDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private Long categoryId;
        private String content;

        private DueType dueType;
        private LocalDate dueDate; // CUSTOM일 때만 사용

        private RepeatType repeatType;
        private SegmentationType segmentationType;
        private VerificationType verificationType;
    }

    @Getter
    @Builder
    public static class CreateResponse {
        private Long todoId;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private Long categoryId;
        private String content;

        private DueType dueType;
        private LocalDate dueDate;

        private RepeatType repeatType;
        private SegmentationType segmentationType;
        private VerificationType verificationType;
    }

    @Getter
    @Builder
    public static class UpdateResponse {
        private Long todoId;
    }

    @Getter
    @Builder
    public static class StatusResponse {
        private Long todoId;
        private todoStatus todoStatus;
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private Long todoId;

        private Long categoryId;
        private String categoryName;

        private String content;
        private LocalDate dueDate;

        private DueType dueType;
        private RepeatType repeatType;
        private SegmentationType segmentationType;
        private VerificationType verificationType;

        private todoStatus todoStatus;
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long todoId;

        private Long categoryId;
        private String categoryName;

        private String content;
        private LocalDate dueDate;

        private DueType dueType;
        private RepeatType repeatType;
        private SegmentationType segmentationType;
        private VerificationType verificationType;

        private todoStatus todoStatus;
    }

    /**
     * FAIL/GIVE_UP 사유 저장용 (일반 투두에서만 필수)
     * 재도전 투두 FAIL일 때는 request가 없어도 됨(서비스에서 정리)
     */
    @Getter
    @NoArgsConstructor
    public static class OutcomeRequest {
        private ReasonCode reasonCode;
        private String customText;
    }

    @Getter
    @NoArgsConstructor
    public static class RetryRequest {
        private DueType dueType;           // TOMORROW or CUSTOM
        private LocalDate dueDate;         // CUSTOM일 때 필수
    }

    @Getter
    @Builder
    public static class RetryResponse {
        private Long originalTodoId;
        private todoStatus originalTodoStatus;

        private Long newTodoId;
        private DueType newDueType;
        private LocalDate newDueDate;
    }

    // 포기 누른 뒤 "재도전 YES"를 한 번에 처리
    @Getter
    @NoArgsConstructor
    public static class GiveUpAndRetryRequest {
        private ReasonCode reasonCode;
        private String customText;

        private DueType dueType;   // TOMORROW or CUSTOM
        private LocalDate dueDate; // CUSTOM일 때 필수
    }

    // 인증 DTO
    @Getter
    @NoArgsConstructor
    public static class PhotoProofRequest {
        private String photoUrl;
    }

    @Getter
    @NoArgsConstructor
    public static class TagRequest {
        private String tagText;
    }

    @Getter
    @Builder
    public static class VerificationResponse {
        private Long todoId;
        private todoStatus todoStatus;
        private LocalDateTime verifiedAt;
    }

    @Getter
    @Builder
    public static class ListResponse {
        private List<SummaryResponse> todos;
    }

    // 시간 기록 (타이머)

    @Getter
    @NoArgsConstructor
    public static class TimeLogCreateRequest {
        private Long durationSeconds;
    }

    @Getter
    @Builder
    public static class TimeLogCreateResponse {
        private Long timeLogId;
        private Long todoId;
        private Long durationSeconds;
        private Long totalSeconds;
    }

    @Getter
    @Builder
    public static class TimeLogSummaryResponse {
        private Long todoId;
        private Long totalSeconds;
    }

    @Getter
    @Builder
    public static class DailyTimeSummaryResponse {
        private LocalDate date;
        private Long totalSeconds;
    }

    // 주차 리포트

    @Getter
    @Builder
    public static class HourlyTimeStat {
        private Integer hour;       // 0~23
        private Long totalSeconds;
    }

    @Getter
    @Builder
    public static class WeeklyTimeReportResponse {
        private Integer year;
        private Integer week;
        private Long totalSeconds;
        private List<HourlyTimeStat> topHours;  // TOP3
    }
}

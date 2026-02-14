package com.example.volrabbit_be.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

public class OnboardingDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OnboardingQuestionResponse {

        private int questionNo;
        private String questionText;
    }

    @Getter
    @NoArgsConstructor
    public static class OnboardingSubmitRequest {

        @NotNull(message = "질문 번호는 필수입니다.")
        @Positive(message = "질문 번호는 양수여야 합니다.")
        private int questionNo;

        @NotNull(message = "응답 번호는 필수입니다.")
        @Range(min = 0, max = 3, message = "응답 번호는 0~3 사이의 정수여야 합니다.")
        private int questionResponse;
    }

    @Getter
    @Builder
    public static class OnboardingResultResponse {

        private long userId;
        private long personaId;

        public static OnboardingResultResponse of(long userId, long personaId) {
            return OnboardingResultResponse.builder()
                    .userId(userId).personaId(personaId).build();
        }
    }
}

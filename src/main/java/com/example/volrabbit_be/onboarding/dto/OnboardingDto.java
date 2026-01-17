package com.example.volrabbit_be.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

        private int questionNo;
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

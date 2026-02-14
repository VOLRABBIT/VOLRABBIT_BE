package com.example.volrabbit_be.onboarding.controller;

import com.example.volrabbit_be.global.common.ApiResponse;
import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.onboarding.dto.OnboardingDto;
import com.example.volrabbit_be.onboarding.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "온보딩 관련 API (온보딩 질문 조회, 온보딩 진단 제출)")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/questions")
    @Operation(summary = "온보딩 질문 조회", description = "온보딩 진단에 필요한 질문들을 한꺼번에 가져옴")
    public ApiResponse<List<OnboardingDto.OnboardingQuestionResponse>> getAllQuestions() {
        List<OnboardingDto.OnboardingQuestionResponse> data = onboardingService.getAllQuestions();

        return ApiResponse.success(data);
    }

    @PostMapping("/submit")
    @Operation(summary = "온보딩 진단 제출 및 페르소나 지정", description = "온보딩 진단 제출 검사를 분석하여 페르소나 지정 및 해당 페르소나 id 반환")
    public ApiResponse<OnboardingDto.OnboardingResultResponse> submitResponses(
            @RequestBody(required = false) List<OnboardingDto.@Valid OnboardingSubmitRequest> request) {

        long userId = SecurityUtil.getCurrentUserId();
        OnboardingDto.OnboardingResultResponse data = onboardingService.submitResponses(userId, request);

        return ApiResponse.success(data);
    }
}

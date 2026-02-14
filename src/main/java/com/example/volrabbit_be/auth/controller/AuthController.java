package com.example.volrabbit_be.auth.controller;

import com.example.volrabbit_be.auth.service.AuthService;
import com.example.volrabbit_be.global.common.ApiResponse;
import com.example.volrabbit_be.global.util.CookieUtil;
import com.example.volrabbit_be.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API (소셜 로그인)")
public class AuthController {

    private final AuthService authService;
    private static final int REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 7; // 7일

    @GetMapping("/oauth/{provider}")
    @Operation(summary = "소셜 로그인 시도", description = "deviceId 필수. 리다이렉트 전 쿠키에 임시 저장함.")
    public void socialLogin(
            @PathVariable String provider,
            @RequestParam String deviceId,
            HttpServletResponse response
    ) throws IOException {
        CookieUtil.addCookie(response, "temp_device_id", deviceId, 300);

        String redirectUrl = authService.getSocialLoginUrl(provider);
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/oauth/callback/{provider}")
    @Operation(summary = "소셜 로그인 완료 (Callback)")
    public ApiResponse<UserDto.TokenResponse> socialLoginCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @CookieValue(value = "temp_device_id", required = false) String deviceId, // [핵심] 쿠키에서 꺼냄
            HttpServletResponse response
    ) {
        if (deviceId == null) {
            throw new IllegalArgumentException("기기 정보가 없습니다. 처음부터 다시 시도해주세요.");
        }

        UserDto.TokenResponse tokenResponse = authService.processSocialLogin(provider, code, deviceId);

        CookieUtil.deleteCookie(response, "temp_device_id");

        CookieUtil.addCookie(response, "refresh_token", tokenResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ApiResponse.success(tokenResponse.clearRefreshToken());
    }
}
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
    @Operation(summary = "소셜 로그인 시도", description = "각 소셜 로그인 페이지로 리다이렉트")
    public void socialLogin(
            @PathVariable String provider,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = authService.getSocialLoginUrl(provider);
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/oauth/callback/{provider}")
    @Operation(summary = "소셜 로그인 완료 (Callback)")
    public ApiResponse<UserDto.TokenResponse> socialLoginCallback(
            @PathVariable String provider,
            @RequestParam String code,
            HttpServletResponse response
    ) {
        // deviceId 없이 깔끔하게 code만 넘겨서 처리합니다.
        UserDto.TokenResponse tokenResponse = authService.processSocialLogin(provider, code);

        CookieUtil.addCookie(response, "refresh_token", tokenResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ApiResponse.success(tokenResponse);
    }
}
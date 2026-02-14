package com.example.volrabbit_be.user.controller;

import com.example.volrabbit_be.global.common.ApiResponse;
import com.example.volrabbit_be.global.util.CookieUtil;
import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.user.dto.UserDto;
import com.example.volrabbit_be.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 관련 API (회원가입, 로그인, 정보 관리)")
public class UserController {

    private final UserService userService;

    private static final int REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 7;

    @PostMapping("/signup")
    @Operation(summary = "일반 회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입 및 자동 로그인")
    public ApiResponse<UserDto.SignupResponse> signup(
                                                       @RequestBody @Valid UserDto.JoinRequest request
    ) {
        UserDto.SignupResponse signupResponse = userService.registerGeneral(request);
        return ApiResponse.success(signupResponse);
    }

    @PostMapping("/login")
    public ApiResponse<UserDto.TokenResponse> login(
            @RequestBody @Valid UserDto.LoginRequest request,
            HttpServletResponse response
    ) {
        UserDto.TokenResponse tokenResponse = userService.loginGeneral(request);
        CookieUtil.addCookie(response, "refresh_token", tokenResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용한 Access Token 갱신")
    public ApiResponse<UserDto.TokenResponse> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh Token이 존재하지 않습니다.");
        }
        UserDto.ReissueRequest request = new UserDto.ReissueRequest(refreshToken);
        UserDto.TokenResponse tokenResponse = userService.reissueToken(request);

        CookieUtil.addCookie(response, "refresh_token", tokenResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "서버 DB의 Refresh Token을 삭제하여 로그아웃 처리")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        userService.logout();
        CookieUtil.deleteCookie(response, "refresh_token");

        return ApiResponse.success(null);
    }


    @GetMapping("/nickname/check")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임 중복 여부 확인 (true: 중복됨, false: 사용 가능)")
    public ApiResponse<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = userService.checkNicknameDuplicate(nickname);
        return ApiResponse.success(isDuplicate);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "토큰을 기반으로 현재 로그인한 유저의 상세 정보 조회")
    public ApiResponse<UserDto.UserInfo> getMyInfo() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(userService.getUserInfo(currentUserId));
    }

    @PatchMapping("/me/nickname")
    @Operation(summary = "닉네임 수정", description = "로그인한 유저의 닉네임 변경 (중복 불가)")
    public ApiResponse<UserDto.UserInfo> updateNickname(@RequestBody UserDto.NicknameUpdateRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(userService.updateNickname(currentUserId, request));
    }

    @PatchMapping("/persona")
    public ApiResponse<UserDto.UserInfo> updatePersona(@RequestBody UserDto.PersonaUpdateRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(userService.updatePersona(currentUserId, request.getPersonaId()));
    }

    @DeleteMapping("/me／delete")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 유저의 계정과 토큰 정보를 영구 삭제")
    public ApiResponse<UserDto.DeleteUserResponse> deleteUser(HttpServletResponse response) {
        UserDto.DeleteUserResponse deleteResponse = userService.deleteUser();
        CookieUtil.deleteCookie(response, "refresh_token");

        return ApiResponse.success(deleteResponse);
    }
}
package com.example.volrabbit_be.user.controller;

import com.example.volrabbit_be.global.common.ApiResponse;
import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.user.dto.UserDto;
import com.example.volrabbit_be.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 관련 API (회원가입, 로그인, 정보 관리)")
public class UserController {

    private final UserService userService;

    // 게스트 회원가입
@PostMapping("/guest")
    @Operation(summary = "게스트 회원가입", description = "랜덤 닉네임 기반 게스트 유저 생성 및 Access/Refresh Token 발급")
    public ApiResponse<UserDto.LoginResponse> registerGuest() {
        UserDto.LoginResponse response = userService.registerGuest();
        return ApiResponse.success(response);
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용한 Access Token 갱신")
    public ApiResponse<UserDto.LoginResponse> reissue(@RequestBody UserDto.ReissueRequest request) {
        UserDto.LoginResponse response = userService.reissueToken(request);
        return ApiResponse.success(response);
    }

    // 닉네임 중복 확인
    @GetMapping("/nickname/check")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임 중복 여부 확인 (true: 중복됨, false: 사용 가능)")
    public ApiResponse<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = userService.checkNicknameDuplicate(nickname);
        return ApiResponse.success(isDuplicate);
    }

    // 내 정보 조회
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "토큰을 기반으로 현재 로그인한 유저의 상세 정보 조회")
    public ApiResponse<UserDto.Response> getMyInfo() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        UserDto.Response response = userService.getUserInfo(currentUserId);
        return ApiResponse.success(response);
    }

    // 닉네임 수정
    @PatchMapping("/me/nickname")
    @Operation(summary = "닉네임 수정", description = "로그인한 유저의 닉네임 변경 (중복 불가)")
    public ApiResponse<UserDto.Response> updateNickname(@RequestBody UserDto.UpdateNicknameRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        UserDto.Response response = userService.updateNickname(currentUserId, request);
        return ApiResponse.success(response);
    }

    // 로그아웃
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "서버 DB의 Refresh Token을 삭제하여 로그아웃 처리")
    public ApiResponse<Void> logout() {
        userService.logout();
        return ApiResponse.success(null);
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 유저의 계정과 토큰 정보를 영구 삭제")
    public ApiResponse<Void> deleteUser() {
        userService.deleteUser();
        return ApiResponse.success(null);
    }
}
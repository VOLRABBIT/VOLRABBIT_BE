package com.example.volrabbit_be.user.dto;

import com.example.volrabbit_be.user.domain.SocialType;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.domain.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    // 토큰 발급 응답 (로그인, 소셜로그인, 토큰재발급 공통 사용)
    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TokenResponse {
        private Long userId;
        private String nickname;
        private String email;
        private String accessToken;

        @JsonIgnore
        private String refreshToken;

        public static TokenResponse of(String accessToken, String refreshToken, User user) {
            return TokenResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
    }

    // 회원가입 요청 (일반 가입)
    @Getter
    @NoArgsConstructor
    public static class JoinRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String nickname;
    }

    // 로그인 요청 (일반 로그인)
    @Getter
    @NoArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    // 내 정보 조회 응답
    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String nickname;
        private String email;
        private UserType userType;
        private SocialType socialType;
        private Long personaId;
        private Long currentMapId;

        public static UserInfo from(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .userType(user.getUserType())
                    .socialType(user.getSocialType())
                    .personaId(user.getPersonaId())
                    .currentMapId(user.getCurrentMapId())
                    .build();
        }
    }

    // 닉네임 수정 요청
    @Getter
    @NoArgsConstructor
    public static class NicknameUpdateRequest {
        private String nickname;
    }

    // 토큰 재발급 요청
    @Getter
    @NoArgsConstructor
    public static class ReissueRequest {
        private String refreshToken;

        public ReissueRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PersonaUpdateRequest {
        private Long personaId;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SignupResponse {
        private Long userId;
        private String email;
        private String nickname;

        public static SignupResponse from(User user) {
            return SignupResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DeleteUserResponse {
        private Long userId;
        private String message;

        public static DeleteUserResponse from(User user) {
            return DeleteUserResponse.builder()
                    .userId(user.getId())
                    .message("계정이 성공적으로 삭제되었습니다.")
                    .build();
        }
    }
}
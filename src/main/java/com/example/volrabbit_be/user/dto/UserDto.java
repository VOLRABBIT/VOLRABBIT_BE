package com.example.volrabbit_be.user.dto;

import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.domain.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    // 1. 로그인 응답 DTO (Access Token + Refresh Token + 유저 정보)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private Response userInfo;

        public static LoginResponse of(String accessToken, String refreshToken, User user) {
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .userInfo(Response.from(user))
                    .build();
        }
    }

    // 2. 토큰 재발급 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReissueRequest {
        private String refreshToken;
    }

    // 3. 닉네임 수정 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateNicknameRequest {
        private String nickname;
    }

    // 4. 일반 유저 정보 응답 DTO (마이페이지 등)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nickname;
        private UserType userType;
        private Long personaId;
        private Long currentMapId;

        public static Response from(User user) {
            return Response.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .userType(user.getUserType())
                    .personaId(user.getPersonaId())
                    .currentMapId(user.getCurrentMapId())
                    .build();
        }
    }
}
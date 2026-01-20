package com.example.volrabbit_be.auth.service;

import com.example.volrabbit_be.global.jwt.TokenProvider;
import com.example.volrabbit_be.user.domain.SocialType;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.domain.UserToken;
import com.example.volrabbit_be.user.domain.UserType;
import com.example.volrabbit_be.user.dto.UserDto;
import com.example.volrabbit_be.user.repository.UserRepository;
import com.example.volrabbit_be.user.repository.UserTokenRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final TokenProvider tokenProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // === KAKAO CONFIG ===
    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri:}")
    private String kakaoRedirectUri;

    // === GOOGLE CONFIG ===
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:}")
    private String googleRedirectUri;

    // === APPLE CONFIG ===
    @Value("${spring.security.oauth2.client.registration.apple.client-id:}")
    private String appleClientId;
    @Value("${spring.security.oauth2.client.registration.apple.redirect-uri:}")
    private String appleRedirectUri;
    @Value("${spring.security.oauth2.client.registration.apple.client-secret:}")
    private String appleClientSecret;

    // 소셜 로그인 인증 URL 생성
    public String getSocialLoginUrl(String provider) {
        return switch (provider.toUpperCase()) {
            case "KAKAO" -> "https://kauth.kakao.com/oauth/authorize"
                    + "?client_id=" + kakaoClientId
                    + "&redirect_uri=" + kakaoRedirectUri
                    + "&response_type=code";
            case "GOOGLE" -> "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + googleClientId
                    + "&redirect_uri=" + googleRedirectUri
                    + "&response_type=code"
                    + "&scope=email%20profile";
            case "APPLE" -> "https://appleid.apple.com/auth/authorize"
                    + "?client_id=" + appleClientId
                    + "&redirect_uri=" + appleRedirectUri
                    + "&response_type=code"
                    + "&response_mode=form_post"
                    + "&scope=name%20email";
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }

    // 소셜 로그인 통합 처리 (핵심 비즈니스 로직)
    @Transactional
    public UserDto.TokenResponse processSocialLogin(String provider, String code, String deviceId) {

        SocialUserInfo userInfo = switch (provider.toUpperCase()) {
            case "KAKAO" -> processKakao(code);
            case "GOOGLE" -> processGoogle(code);
            case "APPLE" -> processApple(code);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };

        String email = userInfo.email();
        String socialId = userInfo.id();
        String nickname = userInfo.nickname();
        SocialType socialType = SocialType.valueOf(provider.toUpperCase());

        Optional<User> existingUser = userRepository.findByEmail(email);

        // 이미 가입된 유저인 경우 (로그인 처리)
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (user.getUserType() == UserType.GENERAL) {
                throw new IllegalArgumentException("이미 일반 이메일로 가입된 계정입니다. 이메일 로그인을 이용해주세요.");
            }

            if (user.getSocialType() != socialType) {
                throw new IllegalArgumentException("이미 " + user.getSocialType() + " 계정으로 가입되어 있습니다.");
            }

            return issueTokens(user);
        }

        // 신규 유저인 경우 (회원가입 처리)
        if (userRepository.existsByDeviceId(deviceId)) {
            throw new IllegalArgumentException("해당 기기에서 이미 가입된 계정이 존재합니다. (1기기 1계정)");
        }

        String finalNickname = (nickname != null && !nickname.isBlank())
                ? nickname
                : "User_" + UUID.randomUUID().toString().substring(0, 8);

        if (userRepository.existsByNickname(finalNickname)) {
            finalNickname += "_" + UUID.randomUUID().toString().substring(0, 4);
        }

        User newUser = User.builder()
                .email(email)
                .nickname(finalNickname)
                .password(null)
                .userType(UserType.SOCIAL)
                .socialType(socialType)
                .socialId(socialId)
                .deviceId(deviceId)
                .build();

        userRepository.save(newUser);
        return issueTokens(newUser);
    }

    private SocialUserInfo processKakao(String code) {
        String accessToken = getKakaoToken(code);
        return getKakaoUserInfo(accessToken);
    }

    private String getKakaoToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoClientId);
        body.add("redirect_uri", kakaoRedirectUri);
        body.add("code", code);

        return requestToken("https://kauth.kakao.com/oauth/token", body, headers);
    }

    private SocialUserInfo getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        ResponseEntity<String> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me", HttpMethod.POST, new HttpEntity<>(headers), String.class
        );

        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            String id = json.get("id").asText();
            String nickname = json.get("properties").get("nickname").asText();
            String email = json.has("kakao_account") && json.get("kakao_account").has("email")
                    ? json.get("kakao_account").get("email").asText()
                    : id + "@kakao.com";

            return new SocialUserInfo(id, email, nickname);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kakao UserInfo Parsing Failed", e);
        }
    }

    private SocialUserInfo processGoogle(String code) {
        String accessToken = getGoogleToken(code);
        return getGoogleUserInfo(accessToken);
    }

    private String getGoogleToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", googleRedirectUri);
        body.add("code", code);

        return requestToken("https://oauth2.googleapis.com/token", body, headers);
    }

    private SocialUserInfo getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo", HttpMethod.GET, new HttpEntity<>(headers), String.class
        );

        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            String id = json.get("sub").asText();
            String email = json.get("email").asText();
            String name = json.get("name").asText();
            return new SocialUserInfo(id, email, name);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Google UserInfo Parsing Failed", e);
        }
    }

    private SocialUserInfo processApple(String code) {
        String idToken = getAppleIdToken(code);
        return decodeAppleIdToken(idToken);
    }

    private String getAppleIdToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", appleClientId);
        body.add("client_secret", appleClientSecret);
        body.add("redirect_uri", appleRedirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://appleid.apple.com/auth/token", HttpMethod.POST, request, String.class
        );

        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("id_token").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Apple Token Parsing Failed", e);
        }
    }

    private SocialUserInfo decodeAppleIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode json = objectMapper.readTree(payload);

            String sub = json.get("sub").asText();
            String email = json.get("email").asText();
            String nickname = email.split("@")[0];

            return new SocialUserInfo(sub, email, nickname);
        } catch (Exception e) {
            throw new RuntimeException("Apple ID Token Decoding Failed", e);
        }
    }

    private String requestToken(String url, MultiValueMap<String, String> body, HttpHeaders headers) {
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("access_token").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Token Parsing Failed", e);
        }
    }

    private UserDto.TokenResponse issueTokens(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), "", Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));

        String accessToken = tokenProvider.generateAccessToken(String.valueOf(user.getId()), authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        UserToken userToken = userTokenRepository.findByUserId(user.getId())
                .orElse(UserToken.builder().user(user).build());

        userToken.updateRefreshToken(refreshToken);
        userTokenRepository.save(userToken);

        return UserDto.TokenResponse.of(accessToken, refreshToken, user);
    }

    private record SocialUserInfo(String id, String email, String nickname) {}
}
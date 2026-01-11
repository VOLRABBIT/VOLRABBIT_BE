package com.example.volrabbit_be.user.service;

import com.example.volrabbit_be.global.jwt.TokenProvider;
import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.domain.UserToken;
import com.example.volrabbit_be.user.domain.UserType;
import com.example.volrabbit_be.user.dto.UserDto;
import com.example.volrabbit_be.user.repository.UserRepository;
import com.example.volrabbit_be.user.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final TokenProvider tokenProvider;

// 게스트 회원가입 및 로그인
    @Transactional
    public UserDto.LoginResponse registerGuest() {
        //  유저 생성
        User user = User.builder()
                .nickname(generateUniqueNickname())
                .userType(UserType.GUEST)
                .build();
        User savedUser = userRepository.save(user);

        //  토큰 발급
        Authentication authentication = createAuthentication(savedUser.getId());
        String accessToken = tokenProvider.generateAccessToken("s",authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // 리프레시 토큰 DB 저장
        UserToken userToken = userTokenRepository.findByUserId(savedUser.getId())
                .orElse(UserToken.builder().user(savedUser).build());
        userToken.updateRefreshToken(refreshToken);
        userTokenRepository.save(userToken);

        return UserDto.LoginResponse.of(accessToken, refreshToken, savedUser);
    }

    // 토큰 재발급
    @Transactional
    public UserDto.LoginResponse reissueToken(UserDto.ReissueRequest request) {
        //  리프레시 토큰 검증
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new RuntimeException("Refresh Token이 유효하지 않습니다.");
        }
        UserToken userToken = userTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("토큰 정보를 찾을 수 없습니다."));

        // 새로운 토큰 발급
        User user = userToken.getUser();
        Authentication authentication = createAuthentication(user.getId());

        String newAccessToken = tokenProvider.generateAccessToken("s",authentication);

        return UserDto.LoginResponse.of(newAccessToken, request.getRefreshToken(), user);
    }

    // 유저 정보 조회
    public UserDto.Response getUserInfo(Long userId) {
        User user = findUserById(userId);
        return UserDto.Response.from(user);
    }

    // 닉네임 수정
    @Transactional
    public UserDto.Response updateNickname(Long userId, UserDto.UpdateNicknameRequest request) {
        User user = findUserById(userId);

        if (checkNicknameDuplicate(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.updateNickname(request.getNickname());
        return UserDto.Response.from(user);
    }

    // 닉네임 중복 확인
    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    // 로그아웃
    @Transactional
    public void logout() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        UserToken userToken = userTokenRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("로그인된 정보가 없습니다."));

        userTokenRepository.delete(userToken);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 연관된 토큰 삭제
        userTokenRepository.findByUserId(currentUserId).ifPresent(userTokenRepository::delete);

        // 유저 삭제
        userRepository.delete(user);
    }

    // 인증 객체 생성
    private Authentication createAuthentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId.toString(),
                "",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // ID로 유저 찾기
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + userId));
    }

    // 랜덤 닉네임 생성
    private String generateUniqueNickname() {
        String nickname;
        do {
            nickname = "Rabbit_" + UUID.randomUUID().toString().substring(0, 5);
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }
}
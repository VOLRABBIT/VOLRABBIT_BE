package com.example.volrabbit_be.user.service;

import com.example.volrabbit_be.global.jwt.TokenProvider;
import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.persona.domain.Persona;
import com.example.volrabbit_be.persona.repository.PersonaRepository;
import com.example.volrabbit_be.user.domain.SocialType;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PersonaRepository personaRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 일반 회원가입
    @Transactional
    public UserDto.SignupResponse registerGeneral(UserDto.JoinRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        if (checkNicknameDuplicate(request.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }
        if (userRepository.existsByDeviceId(request.getDeviceId())) {
            throw new IllegalArgumentException("해당 기기에서 이미 가입된 계정이 존재합니다. (1기기 1계정 정책)");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .userType(UserType.GENERAL)
                .socialType(SocialType.NONE)
                .deviceId(request.getDeviceId())
                .build();

        User savedUser = userRepository.save(user);

        return UserDto.SignupResponse.from(savedUser);
    }

    // 일반 로그인
    @Transactional
    public UserDto.TokenResponse loginGeneral(UserDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (user.getUserType() == UserType.SOCIAL) {
            throw new IllegalArgumentException(
                    "이미 " + user.getSocialType() + " 소셜 계정으로 가입된 이메일입니다. 소셜 로그인을 이용해주세요."
            );
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return issueTokens(user);
    }

    // 소셜 로그인
    @Transactional
    public UserDto.TokenResponse socialLogin(String email, String nickname) {
        return userRepository.findByEmail(email)
                .map(this::issueTokens)
                .orElseGet(() -> {
                    String uniqueNickname = nickname;
                    while (userRepository.existsByNickname(uniqueNickname)) {
                        uniqueNickname = nickname + "_" + UUID.randomUUID().toString().substring(0, 4);
                    }

                    User newUser = User.builder()
                            .email(email)
                            .nickname(uniqueNickname)
                            .password(null)
                            .userType(UserType.SOCIAL)
                            .build();

                    userRepository.save(newUser);
                    return issueTokens(newUser);
                });
    }

    // 내 정보 조회
    public UserDto.UserInfo getUserInfo(Long userId) {
        return UserDto.UserInfo.from(findUserById(userId));
    }

    // 닉네임 수정
    @Transactional
    public UserDto.UserInfo updateNickname(Long userId, UserDto.NicknameUpdateRequest request) {
        User user = findUserById(userId);

        if (checkNicknameDuplicate(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.updateNickname(request.getNickname());
        return UserDto.UserInfo.from(user);
    }

    // 회원 탈퇴
    @Transactional
    public UserDto.DeleteUserResponse deleteUser() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        UserDto.DeleteUserResponse response = UserDto.DeleteUserResponse.from(user);

        userTokenRepository.findByUserId(currentUserId).ifPresent(userTokenRepository::delete);
        userRepository.delete(user);

        return response;
    }

    // 로그아웃
    @Transactional
    public void logout() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        UserToken userToken = userTokenRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("로그인된 정보가 없습니다."));

        userTokenRepository.delete(userToken);
    }


    // 토큰 재발급
    @Transactional
    public UserDto.TokenResponse reissueToken(UserDto.ReissueRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("Refresh Token이 유효하지 않습니다.");
        }
        UserToken userToken = userTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("토큰 정보를 찾을 수 없습니다."));

        User user = userToken.getUser();
        Authentication authentication = createAuthentication(user.getId());

        String newAccessToken = tokenProvider.generateAccessToken(String.valueOf(user.getId()), authentication);

        return UserDto.TokenResponse.of(newAccessToken, request.getRefreshToken(), user);
    }

    // 토큰 발급 공통 메서드
    private UserDto.TokenResponse issueTokens(User user) {
        Authentication authentication = createAuthentication(user.getId());

        String accessToken = tokenProvider.generateAccessToken(String.valueOf(user.getId()), authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        UserToken userToken = userTokenRepository.findByUserId(user.getId())
                .orElse(UserToken.builder().user(user).build());

        userToken.updateRefreshToken(refreshToken);
        userTokenRepository.save(userToken);

        return UserDto.TokenResponse.of(accessToken, refreshToken, user);
    }


    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    private Authentication createAuthentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId.toString(),
                "",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + userId));
    }

    @Transactional
    public UserDto.UserInfo updatePersona(Long userId, Long personaId) {
        User user = findUserById(userId);
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페르소나입니다."));
        user.selectPersona(persona.getId());

        return UserDto.UserInfo.from(user);
    }
}
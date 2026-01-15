package com.example.volrabbit_be.global.jwt;

import com.example.volrabbit_be.global.util.RsaKeyLoader;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";

    // application.yml에서 값 주입 받음
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public TokenProvider(
            @Value("${jwt.access.token.expiration}") long accessTokenValidityInSeconds,
            @Value("${jwt.refresh.token.expiration}") long refreshTokenValidityInSeconds) {
        try{
            ClassPathResource privateKeyResource = new ClassPathResource("keys/private.pem");
            ClassPathResource publicKeyResource = new ClassPathResource("keys/public.pem");
            this.privateKey = RsaKeyLoader.loadPrivateKey(privateKeyResource.getInputStream());
            this.publicKey = RsaKeyLoader.loadPublicKey(publicKeyResource.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("키 파일 로드 실패", e);
        }

        this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
    }

    // Access Token 생성
    public String generateAccessToken(String userId, Authentication authentication) {
        return generateToken(userId, authentication, accessTokenValidityInMilliseconds);
    }

    // Refresh Token 생성
    public String generateRefreshToken(Authentication authentication) {
        long now = (new Date()).getTime();

        return Jwts.builder()
                .setSubject(authentication.getName()) // 핵심: 유저 ID(PK)를 넣는다!
                .setExpiration(new Date(now + refreshTokenValidityInMilliseconds))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // 내부 공통 토큰 생성 메서드
    private String generateToken(String userId, Authentication authentication, long validity) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validityTime = new Date(now + validity);

        return Jwts.builder()
                .setSubject(userId)
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(validityTime)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // 토큰 정보 조회
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
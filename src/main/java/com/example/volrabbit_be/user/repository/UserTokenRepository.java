package com.example.volrabbit_be.user.repository;

import com.example.volrabbit_be.user.domain.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByRefreshToken(String refreshToken);
    Optional<UserToken> findByUserId(Long userId);
}
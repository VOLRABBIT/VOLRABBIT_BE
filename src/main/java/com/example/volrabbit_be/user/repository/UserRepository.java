package com.example.volrabbit_be.user.repository;

import com.example.volrabbit_be.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname(String nickname);
    Optional<User> findByNickname(String nickname);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
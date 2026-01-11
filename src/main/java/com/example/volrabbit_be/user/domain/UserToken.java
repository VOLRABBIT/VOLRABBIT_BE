package com.example.volrabbit_be.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_tokens")
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String refreshToken;

    @Builder
    public UserToken(User user, String refreshToken) {
        this.user = user;
        this.refreshToken = refreshToken;
    }

    public void updateRefreshToken(String newToken) {
        this.refreshToken = newToken;
    }
}
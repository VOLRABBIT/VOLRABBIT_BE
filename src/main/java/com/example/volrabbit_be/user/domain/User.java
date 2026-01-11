package com.example.volrabbit_be.user.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType; // GUEST, SOCIAL

    private Long personaId;

    private Long currentMapId;

    @Builder
    public User(String nickname, UserType userType) {
        this.nickname = nickname;
        this.userType = userType;
    }

    // 닉네임 변경 (나중에 사용자가 직접 수정할 때 사용)
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    // 현재 플레이 중인 맵 변경
    public void updateCurrentMap(Long mapId) {
        this.currentMapId = mapId;
    }

    // 페르소나 설정
    public void selectPersona(Long personaId) {
        this.personaId = personaId;
    }
}
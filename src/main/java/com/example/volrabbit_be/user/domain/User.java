package com.example.volrabbit_be.user.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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

    @Email
    @Column(unique = true, length = 50)
    private String email;

    @Column(length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    private String socialId;

    @Column(unique = true)
    private String deviceId;

    private Long personaId;

    private Long currentMapId;

    @Builder
    public User(String email, String password, String nickname, UserType userType,
                SocialType socialType, String socialId, String deviceId,
                Long personaId, Long currentMapId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.userType = userType;
        this.socialType = socialType;
        this.socialId = socialId;
        this.deviceId = deviceId;
        this.personaId = personaId;
        this.currentMapId = currentMapId;
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void updateCurrentMap(Long mapId) {
        this.currentMapId = mapId;
    }

    public void selectPersona(Long personaId) {
        this.personaId = personaId;
    }
}
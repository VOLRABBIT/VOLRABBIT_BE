package com.example.volrabbit_be.guild.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import com.example.volrabbit_be.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "guild_members")
public class GuildMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuildRole role;

    @Builder
    public GuildMember(Guild guild, User user, GuildRole role) {
        this.guild = guild;
        this.user = user;
        this.role = role;
    }

    public enum GuildRole {
        LEADER, MEMBER
    }
}

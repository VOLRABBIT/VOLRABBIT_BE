package com.example.volrabbit_be.guild.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "guilds")
public class Guild extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(length = 50)
    private String introduction;

    @Column(nullable = false)
    private boolean isPrivate;

    private String password;

    private int memberCount;

    @Builder
    public Guild(String name, String introduction, boolean isPrivate, String password) {
        this.name = name;
        this.introduction = introduction;
        this.isPrivate = isPrivate;
        this.password = password;
        this.memberCount = 1; // Default to 1 (creator)
    }

    public void updateguild(String name, String introduction, Boolean isPrivate, String password) {
        if (name != null)
            this.name = name;
        if (introduction != null)
            this.introduction = introduction;
        if (isPrivate != null) {
            this.isPrivate = isPrivate;
            if (!this.isPrivate) {
                this.password = null;
            }
        }
        if (this.isPrivate && password != null) {
            this.password = password;
        }
    }

    public void increaseMemberCount() {
        this.memberCount++;
    }

    public void decreaseMemberCount() {
        if (this.memberCount > 0) {
            this.memberCount--;
        }
    }
}

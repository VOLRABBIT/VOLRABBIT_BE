package com.example.volrabbit_be.todo.domain;

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
@Table(name = "categories")
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false)
    private boolean isActive; // 연결된 task가 있을 경우, 삭제 대신 비활성화

    @Builder
    private Category(User user, String name) {   // isActive는 외부에서 안 받게
        this.user = user;
        this.name = name;
        this.isActive = true; // true 명시
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void rename(String name) {
        this.name = name;
    }
}

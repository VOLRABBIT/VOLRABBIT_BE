package com.example.volrabbit_be.todo.domain;

// TODO 인증

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "todo_verifications")
public class TodoVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column
    private String photoUrl;

    @Column(nullable = false)
    private boolean checked;

    @Builder
    private TodoVerification(Todo todo) {
        this.todo = todo;
        this.checked = false;
    }

    // 체크 인증 성공
    public void verifyByCheck() {
        this.checked = true;
    }

    // 사진 인증 성공
    public void verifyByPhoto(String photoUrl) {
        this.photoUrl = photoUrl;
        this.checked = true;
    }

    // NFC 인증 성공 (따로 저장할 값은 없고, 인증 여부만 boolean으로 체크)
    public void verifyByNfc() {
        this.checked = true;
    }

    // 인증 여부 확인
    public boolean isVerified() {
        return checked;
    }
}

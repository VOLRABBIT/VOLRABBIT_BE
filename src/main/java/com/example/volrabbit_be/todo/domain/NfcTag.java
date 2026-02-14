package com.example.volrabbit_be.todo.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import com.example.volrabbit_be.todo.domain.enumType.NfcTagStatus;
import com.example.volrabbit_be.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "nfc_tag")
public class NfcTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 투두 1개당 NFC 태그 1개만 등록 (A 방식)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false, unique = true)
    private Todo todo;

    // 소유자(보안 체크용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tag_code", nullable = false, unique = true, length = 36)
    private String tagCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NfcTagStatus status;

    private NfcTag(Todo todo, User user, String tagCode) {
        this.todo = todo;
        this.user = user;
        this.tagCode = tagCode;
        this.status = NfcTagStatus.ACTIVE;
    }

    public static NfcTag create(Todo todo, User user, String tagCode) {
        return new NfcTag(todo, user, tagCode);
    }

    public boolean isActive() {
        return status == NfcTagStatus.ACTIVE;
    }

    public void revoke() {
        this.status = NfcTagStatus.REVOKED;
    }
}

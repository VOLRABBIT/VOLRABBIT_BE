package com.example.volrabbit_be.todo.domain;

// 세분화

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "todo_steps")
public class TodoStep extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean isCompleted;

    @Builder
    private TodoStep(Todo todo, String content) {
        this.todo = todo;
        this.content = content;
        this.isCompleted = false;
    }

    // 단계 내용 수정
    public void updateContent(String content) {
        this.content = content;
    }

    // 단계 완료 처리
    public void complete() {
        this.isCompleted = true;
    }

    // 단계 완료 취소
    public void undoComplete() {
        this.isCompleted = false;
    }

    // 완료 상태
    public void toggleComplete() {
        this.isCompleted = !this.isCompleted;
    }
}

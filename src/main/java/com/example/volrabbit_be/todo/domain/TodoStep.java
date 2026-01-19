package com.example.volrabbit_be.todo.domain;

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

    @Column(nullable = false)
    private Long stepOrder;

    @Builder
    private TodoStep(Todo todo, String content, Long stepOrder) {
        this.todo = todo;
        this.content = content;
        this.stepOrder = stepOrder;
        this.isCompleted = false;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void toggleComplete() {
        this.isCompleted = !this.isCompleted;
    }
}

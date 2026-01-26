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
@Table(name = "todo_time_logs")
public class TodoTimeLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @Builder
    private TodoTimeLog(Todo todo, Long durationSeconds) {
        this.todo = todo;
        this.durationSeconds = durationSeconds;
    }
}

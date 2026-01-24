package com.example.volrabbit_be.todo.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import com.example.volrabbit_be.todo.domain.enumType.PriorityQuadrant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "todo_priorities")
public class TodoPriority extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId 검증은 todo.user로 가능해서 user_id 별도 저장 안 함(중복 방지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column(name = "priority_date", nullable = false)
    private LocalDate priorityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_quadrant", nullable = false)
    private PriorityQuadrant priorityQuadrant;

    @Column(name = "priority_order", nullable = false)
    private Integer priorityOrder;

    @Builder
    private TodoPriority(Todo todo, LocalDate priorityDate, PriorityQuadrant priorityQuadrant, Integer priorityOrder) {
        this.todo = todo;
        this.priorityDate = priorityDate;
        this.priorityQuadrant = priorityQuadrant;
        this.priorityOrder = priorityOrder;
    }

    public void update(Integer priorityOrder, PriorityQuadrant quadrant) {
        this.priorityOrder = priorityOrder;
        this.priorityQuadrant = quadrant;
    }

    public void updateOrder(Integer priorityOrder) {
        this.priorityOrder = priorityOrder;
    }
}

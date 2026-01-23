package com.example.volrabbit_be.todo.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import com.example.volrabbit_be.todo.domain.enumType.*;
import com.example.volrabbit_be.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "todos")
public class Todo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 10)
    private String content;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DueType dueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatType repeatType;

    @Column(name = "repeat_group_id", length = 36)
    private String repeatGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentationType segmentationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private todoStatus todoStatus;

    // 재도전으로 생성된 투두면 원본 todo id가 들어감 (일반 투두는 null)
    @Column(name = "retry_from_todo_id")
    private Long retryFromTodoId;

    @Builder
    private Todo(
            User user,
            Category category,
            String content,
            LocalDate dueDate,
            DueType dueType,
            RepeatType repeatType,
            String repeatGroupId,
            SegmentationType segmentationType,
            VerificationType verificationType,
            Long retryFromTodoId
    ) {
        this.user = user;
        this.category = category;
        this.content = content;
        this.dueDate = dueDate;
        this.dueType = dueType;
        this.repeatType = repeatType;
        this.repeatGroupId = repeatGroupId;
        this.segmentationType = segmentationType;
        this.verificationType = verificationType;
        this.todoStatus = todoStatus.IN_PROGRESS;
        this.retryFromTodoId = retryFromTodoId;
    }

    public boolean isRetryTodo() {
        return this.retryFromTodoId != null;
    }

    public void update(
            Category category,
            String content,
            LocalDate dueDate,
            DueType dueType,
            RepeatType repeatType,
            String repeatGroupId,
            SegmentationType segmentationType,
            VerificationType verificationType
    ) {
        this.category = category;
        this.content = content;
        this.dueDate = dueDate;
        this.dueType = dueType;
        this.repeatType = repeatType;
        this.repeatGroupId = repeatGroupId;
        this.segmentationType = segmentationType;
        this.verificationType = verificationType;
    }

    public void patchUpdate(
            Category category,
            String content,
            LocalDate dueDate,
            DueType dueType,
            RepeatType repeatType,
            String repeatGroupId,
            SegmentationType segmentationType,
            VerificationType verificationType
    ) {
        if (category != null) this.category = category;
        if (content != null) this.content = content;
        if (dueType != null) this.dueType = dueType;
        if (dueDate != null) this.dueDate = dueDate;
        if (repeatType != null) this.repeatType = repeatType;
        if (repeatGroupId != null || (repeatType == RepeatType.NONE)) {
            this.repeatGroupId = repeatGroupId;
        }
        if (segmentationType != null) this.segmentationType = segmentationType;
        if (verificationType != null) this.verificationType = verificationType;
    }

    public void complete() {
        if (this.todoStatus != todoStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 할 일만 완료 처리할 수 있습니다.");
        }
        this.todoStatus = todoStatus.SUCCESS;
    }

    public void fail() {
        if (this.todoStatus != todoStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 할 일만 실패 처리할 수 있습니다.");
        }
        this.todoStatus = todoStatus.FAIL;
    }

    public void reopenToInProgress() {
        if (this.todoStatus != todoStatus.SUCCESS) {
            throw new IllegalStateException("완료된 할 일만 미완료로 되돌릴 수 있습니다.");
        }
        this.todoStatus = todoStatus.IN_PROGRESS;
    }

    public void giveUp() {
        if (this.todoStatus != todoStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 할 일만 포기할 수 있습니다.");
        }
        this.todoStatus = todoStatus.GIVE_UP;
    }

    public Todo createRetryTodo(LocalDate newDueDate, DueType newDueType) {
        if (this.todoStatus != todoStatus.FAIL && this.todoStatus != todoStatus.GIVE_UP) {
            throw new IllegalStateException("실패/포기한 할 일만 다시 도전할 수 있습니다.");
        }

        return Todo.builder()
                .user(this.user)
                .category(this.category)
                .content(this.content)
                .dueDate(newDueDate)
                .dueType(newDueType)
                .repeatType(this.repeatType)
                .repeatGroupId(this.repeatGroupId)
                .segmentationType(this.segmentationType)
                .verificationType(this.verificationType)
                .retryFromTodoId(this.id)
                .build();
    }
}

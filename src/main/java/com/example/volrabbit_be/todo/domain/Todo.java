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
@Table(name = "Todos")
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
    private DueType dueType; // TODAY, TOMORROW, CUSTOM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatType repeatType; // NONE, DAILY, WEEKLY, MONTHLY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentationType segmentationType; // NONE, TWO, THREE, AUTO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType verificationType; // CHECK, PHOTO-PROOF, TAG

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private todoStatus todoStatus; // IN_PROGRESS, SUCCESS, FAIL, GIVE_UP, RETRY

    @Builder
    private Todo(
            User user,
            Category category,
            String content,
            LocalDate dueDate,
            DueType dueType,
            RepeatType repeatType,
            SegmentationType segmentationType,
            VerificationType verificationType
    ) {
        this.user = user;
        this.category = category;
        this.content = content;
        this.dueDate = dueDate;
        this.dueType = dueType;
        this.repeatType = repeatType;
        this.segmentationType = segmentationType;
        this.verificationType = verificationType;
        this.todoStatus = todoStatus.IN_PROGRESS;
    }

    // 투두 수정시, 전체 수정 저장용
    public void update(
            Category category,
            String content,
            LocalDate dueDate,
            DueType dueType,
            RepeatType repeatType,
            SegmentationType segmentationType,
            VerificationType verificationType
    ) {
        this.category = category;
        this.content = content;
        this.dueDate = dueDate;
        this.dueType = dueType;
        this.repeatType = repeatType;
        this.segmentationType = segmentationType;
        this.verificationType = verificationType;
    }

    // 상태 변경
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

    public void giveUp() {
        if (this.todoStatus == todoStatus.SUCCESS) {
            throw new IllegalStateException("이미 완료된 할 일은 포기할 수 없습니다.");
        }
        this.todoStatus = todoStatus.GIVE_UP;
    }

    /**
     * 다시 도전하기:
     * - 기존 투두는 FAIL/GIVE_UP 상태로 "저장"되어 남아있고
     * - 선택한 날짜로 새로운 투두를 생성한다 (새 투두는 IN_PROGRESS)
     */
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
                .segmentationType(this.segmentationType)
                .verificationType(this.verificationType)
                .build();
    }
}

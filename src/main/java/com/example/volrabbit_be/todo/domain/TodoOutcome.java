package com.example.volrabbit_be.todo.domain;

// FAIL OR GIVE_UP 원인

import com.example.volrabbit_be.todo.domain.enumType.OutcomeStatus;
import com.example.volrabbit_be.todo.domain.enumType.ReasonCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "todo_outcomes")
public class TodoOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutcomeStatus outcomeStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReasonCode reasonCode; // DONT_KNOW_HOW, TOO_COMPLEX, NO_TIME, INTERRUPTED, ETC

    // 원인이 etc일 경우
    @Column
    private String customText;

    @Builder
    private TodoOutcome(
            Todo todo,
            OutcomeStatus outcomeStatus,
            ReasonCode reasonCode,
            String customText
    ) {
        this.todo = todo;
        this.outcomeStatus = outcomeStatus;
        this.reasonCode = reasonCode;

        validateCustomText(reasonCode, customText);
        this.customText = customText;
    }

    // 내부 검증 규칙
    private void validateCustomText(ReasonCode reasonCode, String customText) {
        if (reasonCode == ReasonCode.ETC && (customText == null || customText.isBlank())) {
            throw new IllegalArgumentException("기타 선택시 사유 작성은 필수입니다.");
        }

        if (reasonCode != ReasonCode.ETC && customText != null) {
            throw new IllegalArgumentException("기타를 선택하지 않은 경우 사유 작성은 불가능합니다.");
        }
    }
}

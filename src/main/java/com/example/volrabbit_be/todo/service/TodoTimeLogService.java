package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoTimeLog;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.todo.repository.TodoTimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoTimeLogService {

    private static final long MAX_SECONDS = 12 * 60 * 60; // 12시간

    private final TodoRepository todoRepository;
    private final TodoTimeLogRepository todoTimeLogRepository;

    @Transactional
    public TodoDto.TimeLogCreateResponse create(
            Long userId,
            Long todoId,
            TodoDto.TimeLogCreateRequest request
    ) {
        if (request == null || request.getDurationSeconds() == null) {
            throw new IllegalArgumentException("durationSeconds는 필수입니다.");
        }

        long seconds = request.getDurationSeconds();

        if (seconds <= 0) {
            throw new IllegalArgumentException("durationSeconds는 0보다 커야 합니다.");
        }
        if (seconds > MAX_SECONDS) {
            throw new IllegalArgumentException("durationSeconds가 너무 큽니다.");
        }

        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("투두가 존재하지 않습니다."));

        TodoTimeLog log = TodoTimeLog.builder()
                .todo(todo)
                .durationSeconds(seconds)
                .build();

        TodoTimeLog saved = todoTimeLogRepository.save(log);

        long totalSeconds = todoTimeLogRepository.sumDurationSecondsByTodoId(todoId);

        return TodoDto.TimeLogCreateResponse.builder()
                .timeLogId(saved.getId())
                .todoId(todoId)
                .durationSeconds(seconds)
                .totalSeconds(totalSeconds)
                .build();
    }

    public TodoDto.TimeLogSummaryResponse summary(Long userId, Long todoId) {
        todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("투두가 존재하지 않습니다."));

        long totalSeconds = todoTimeLogRepository.sumDurationSecondsByTodoId(todoId);

        return TodoDto.TimeLogSummaryResponse.builder()
                .todoId(todoId)
                .totalSeconds(totalSeconds)
                .build();
    }
}

package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.global.exception.ApiExceptions;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoStep;
import com.example.volrabbit_be.todo.domain.enumType.todoStatus;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.todo.repository.TodoStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoStepService {

    private final TodoRepository todoRepository;
    private final TodoStepRepository todoStepRepository;

    /**
     * 특정 투두의 세분화 step 목록 조회
     */
    public TodoDto.StepListResponse getSteps(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("투두가 존재하지 않습니다."));

        List<TodoDto.StepResponse> steps = todoStepRepository
                .findAllByTodoIdOrderByStepOrderAsc(todo.getId())
                .stream()
                .map(this::toStepResponse)
                .toList();

        return TodoDto.StepListResponse.builder()
                .todoId(todo.getId())
                .steps(steps)
                .build();
    }

    /**
     * todo_step을 토글하고, 모든 step이 완료되면 todo를 자동으로 SUCCESS 처리
     */
    @Transactional
    public TodoDto.StatusResponse toggleStep(Long userId, Long todoId, Long stepId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("투두가 존재하지 않습니다."));

        TodoStep step = todoStepRepository.findById(stepId)
                .orElseThrow(() -> new ApiExceptions.NotFound("세분화 단계가 존재하지 않습니다."));

        if (!step.getTodo().getId().equals(todo.getId())) {
            throw new ApiExceptions.BadRequest("해당 투두의 세분화 단계가 아닙니다.");
        }

        if (todo.getTodoStatus() != todoStatus.IN_PROGRESS) {
            throw new ApiExceptions.Conflict("진행 중인 투두에서만 세분화 단계를 완료할 수 있습니다.");
        }

        step.toggleComplete();

        long remaining = todoStepRepository.countByTodoIdAndIsCompletedFalse(todo.getId());
        if (remaining == 0) {
            todo.complete();
        }

        return TodoDto.StatusResponse.builder()
                .todoId(todo.getId())
                .todoStatus(todo.getTodoStatus())
                .build();
    }

    private TodoDto.StepResponse toStepResponse(TodoStep step) {
        return TodoDto.StepResponse.builder()
                .stepId(step.getId())
                .content(step.getContent())
                .completed(step.isCompleted())
                .stepOrder(step.getStepOrder())
                .build();
    }
}

package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.service.TodoStepService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
public class TodoStepController {

    private final TodoStepService todoStepService;

    // stepId 조회용
    @GetMapping("/{todoId}/steps")
    public ResponseEntity<TodoDto.StepListResponse> getSteps(
            @PathVariable Long todoId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoStepService.getSteps(userId, todoId));
    }

    // todo_step(세분화)을 하나씩 체크/해제, 모든 step 완료 시 todo를 자동 SUCCESS 처리
    @PatchMapping("/{todoId}/steps/{stepId}/toggle")
    public ResponseEntity<TodoDto.StatusResponse> toggleStep(
            @PathVariable Long todoId,
            @PathVariable Long stepId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoStepService.toggleStep(userId, todoId, stepId));
    }

    // TODO: [임시] 나중에 AI 전체 연동 완성 시 삭제 예정 -> 프론트 수동 수정 API
    // todo_step 수동 텍스트 수정
    @PatchMapping("/{todoId}/steps/{stepId}/content")
    public ResponseEntity<TodoDto.StepListResponse> updateStepContent(
            @PathVariable Long todoId,
            @PathVariable Long stepId,
            @Valid @RequestBody TodoDto.StepUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoStepService.updateStep(userId, todoId, stepId, request));
    }
}

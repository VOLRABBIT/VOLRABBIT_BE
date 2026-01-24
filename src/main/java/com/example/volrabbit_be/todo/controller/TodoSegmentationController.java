package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.service.TodoSegmentationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
public class TodoSegmentationController {

    private final TodoSegmentationService todoSegmentationService;

    // 세분화를 위한 AI 호출
    @PostMapping("/{todoId}/steps/ai")
    public ResponseEntity<Void> regenerateSteps(
            @PathVariable Long todoId,
            @RequestBody TodoDto.AiSegmentationRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        todoSegmentationService.generateStepsIfNeeded(userId, todoId, request.getSegmentationType());
        return ResponseEntity.ok().build();
    }
}

package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.service.TodoTimeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos/{todoId}/time-logs")
public class TodoTimeLogController {

    private final TodoTimeLogService todoTimeLogService;

    // 종료 버튼 → 시간 기록 저장
    @PostMapping
    public ResponseEntity<TodoDto.TimeLogCreateResponse> create(
            @PathVariable Long todoId,
            @RequestBody TodoDto.TimeLogCreateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(
                todoTimeLogService.create(userId, todoId, request)
        );
    }

    // 누적 시간 조회
    @GetMapping("/summary")
    public ResponseEntity<TodoDto.TimeLogSummaryResponse> summary(
            @PathVariable Long todoId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(
                todoTimeLogService.summary(userId, todoId)
        );
    }
}

package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.service.TodoTimeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
public class TodoTimeLogController {

    private final TodoTimeLogService todoTimeLogService;

    // 종료 버튼 → 시간 기록 저장
    @PostMapping("/{todoId}/time-logs")
    public ResponseEntity<TodoDto.TimeLogCreateResponse> create(
            @PathVariable Long todoId,
            @RequestBody TodoDto.TimeLogCreateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoTimeLogService.create(userId, todoId, request));
    }

    // 투두별 누적 시간 조회
    @GetMapping("/{todoId}/time-logs/summary")
    public ResponseEntity<TodoDto.TimeLogSummaryResponse> summary(
            @PathVariable Long todoId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoTimeLogService.summary(userId, todoId));
    }

    // 날짜별 누적 시간 조회
    @GetMapping("/time-logs/daily")
    public ResponseEntity<TodoDto.DailyTimeSummaryResponse> daily(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoTimeLogService.dailySummary(userId, date));
    }

    // 주차별 리포트 조회 (총 집중시간 + 생산적 시간대 TOP3)
    @GetMapping("/time-logs/weekly")
    public ResponseEntity<TodoDto.WeeklyTimeReportResponse> weekly(
            @RequestParam int year,
            @RequestParam int week
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoTimeLogService.weeklyReport(userId, year, week));
    }
}

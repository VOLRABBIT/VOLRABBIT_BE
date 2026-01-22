package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.global.exception.ApiExceptions;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoTimeLog;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.todo.repository.TodoTimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;

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
            throw new ApiExceptions.BadRequest("durationSeconds는 필수입니다.");
        }

        long seconds = request.getDurationSeconds();
        if (seconds <= 0) throw new ApiExceptions.BadRequest("durationSeconds는 0보다 커야 합니다.");
        if (seconds > MAX_SECONDS) throw new ApiExceptions.BadRequest("durationSeconds가 너무 큽니다.");

        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("투두가 존재하지 않습니다."));

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
                .orElseThrow(() -> new ApiExceptions.NotFound("투두가 존재하지 않습니다."));

        long totalSeconds = todoTimeLogRepository.sumDurationSecondsByTodoId(todoId);

        return TodoDto.TimeLogSummaryResponse.builder()
                .todoId(todoId)
                .totalSeconds(totalSeconds)
                .build();
    }

    public TodoDto.DailyTimeSummaryResponse dailySummary(Long userId, LocalDate date) {
        if (date == null) throw new ApiExceptions.BadRequest("date는 필수입니다.");

        long totalSeconds = todoTimeLogRepository.sumDurationSecondsByUserIdAndDate(userId, date);

        return TodoDto.DailyTimeSummaryResponse.builder()
                .date(date)
                .totalSeconds(totalSeconds)
                .build();
    }

    public TodoDto.WeeklyTimeReportResponse weeklyReport(Long userId, int year, int week) {
        if (year < 2000 || year > 2100) throw new ApiExceptions.BadRequest("year가 올바르지 않습니다.");
        if (week < 1 || week > 53) throw new ApiExceptions.BadRequest("week가 올바르지 않습니다.");

        WeekFields wf = WeekFields.ISO;

        LocalDate anchor = LocalDate.of(year, 1, 4);
        LocalDate weekStart = anchor
                .with(wf.weekOfWeekBasedYear(), week)
                .with(wf.dayOfWeek(), 1); // 월요일

        LocalDateTime startInclusive = weekStart.atStartOfDay();
        LocalDateTime endExclusive = startInclusive.plusDays(7);

        long totalSeconds = todoTimeLogRepository.sumDurationSecondsByUserIdAndCreatedDateRange(
                userId, startInclusive, endExclusive
        );

        List<TodoTimeLogRepository.HourSum> top = todoTimeLogRepository.findTopHoursByUserIdAndCreatedDateRange(
                userId, startInclusive, endExclusive, PageRequest.of(0, 3)
        );

        List<TodoDto.HourlyTimeStat> topHours = top.stream()
                .map(r -> TodoDto.HourlyTimeStat.builder()
                        .hour(r.getHour())
                        .totalSeconds(r.getTotalSeconds() == null ? 0L : r.getTotalSeconds())
                        .build()
                )
                .toList();

        return TodoDto.WeeklyTimeReportResponse.builder()
                .year(year)
                .week(week)
                .totalSeconds(totalSeconds)
                .topHours(topHours)
                .build();
    }
}

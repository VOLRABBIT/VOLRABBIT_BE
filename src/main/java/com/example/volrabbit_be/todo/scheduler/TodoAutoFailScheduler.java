package com.example.volrabbit_be.todo.scheduler;

import com.example.volrabbit_be.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TodoAutoFailScheduler {

    private final TodoService todoService;

    // 매일 00:00:00 실행 (서버 타임존 기준)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void autoFail() {
        todoService.autoFailExpiredTodos(LocalDate.now());
    }

}
package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.TodoTimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoTimeLogRepository extends JpaRepository<TodoTimeLog, Long> {

    @Query("""
        select coalesce(sum(t.durationSeconds), 0)
        from TodoTimeLog t
        where t.todo.id = :todoId
    """)
    long sumDurationSecondsByTodoId(@Param("todoId") Long todoId);
}

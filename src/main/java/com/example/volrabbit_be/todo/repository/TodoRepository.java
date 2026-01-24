package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.enumType.todoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @EntityGraph(attributePaths = "category")
    List<Todo> findAllByUserIdOrderByDueDateAscIdAsc(Long userId);

    @EntityGraph(attributePaths = "category")
    List<Todo> findAllByUserIdAndDueDateOrderByIdAsc(Long userId, LocalDate dueDate);

    @EntityGraph(attributePaths = "category")
    Optional<Todo> findByIdAndUserId(Long todoId, Long userId);

    // 자정 자동 실패 대상: dueDate < today && status == IN_PROGRESS
    List<Todo> findAllByDueDateBeforeAndTodoStatus(LocalDate date, todoStatus status);
}

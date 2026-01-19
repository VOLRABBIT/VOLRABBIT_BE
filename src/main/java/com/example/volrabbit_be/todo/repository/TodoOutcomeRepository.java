package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.TodoOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TodoOutcomeRepository extends JpaRepository<TodoOutcome, Long> {

    Optional<TodoOutcome> findTopByTodoIdOrderByIdDesc(Long todoId);
}
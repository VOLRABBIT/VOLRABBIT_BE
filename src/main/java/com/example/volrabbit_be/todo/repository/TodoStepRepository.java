package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.TodoStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoStepRepository extends JpaRepository<TodoStep, Long> {

    List<TodoStep> findAllByTodoIdOrderByStepOrderAsc(Long todoId);

    long countByTodoIdAndIsCompletedFalse(Long todoId);

    void deleteAllByTodoId(Long todoId);
}

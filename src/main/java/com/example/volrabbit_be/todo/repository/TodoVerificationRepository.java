package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.TodoVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TodoVerificationRepository extends JpaRepository<TodoVerification, Long> {

    boolean existsByTodoId(Long todoId);

    Optional<TodoVerification> findByTodoId(Long todoId);

    void deleteByTodoId(Long todoId);
}

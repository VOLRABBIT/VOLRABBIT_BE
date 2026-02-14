package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.TodoPriority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoPriorityRepository extends JpaRepository<TodoPriority, Long> {

    List<TodoPriority> findAllByTodoUserIdAndPriorityDateOrderByPriorityOrderAsc(Long userId, LocalDate priorityDate);

    Optional<TodoPriority> findByTodoIdAndPriorityDate(Long todoId, LocalDate priorityDate);

    void deleteAllByTodoUserIdAndPriorityDate(Long userId, LocalDate priorityDate);
}

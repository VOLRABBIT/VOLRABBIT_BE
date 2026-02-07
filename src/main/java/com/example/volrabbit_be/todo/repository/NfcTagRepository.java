package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.NfcTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NfcTagRepository extends JpaRepository<NfcTag, Long> {

    boolean existsByTodoId(Long todoId);

    boolean existsByTagCode(String tagCode);

    Optional<NfcTag> findByTodoId(Long todoId);

    Optional<NfcTag> findByTagCode(String tagCode);
}

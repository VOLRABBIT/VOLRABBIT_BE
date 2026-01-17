package com.example.volrabbit_be.persona.repository;

import com.example.volrabbit_be.persona.domain.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
}

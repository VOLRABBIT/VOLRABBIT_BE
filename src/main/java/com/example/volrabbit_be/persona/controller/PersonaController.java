package com.example.volrabbit_be.persona.controller;

import com.example.volrabbit_be.global.common.ApiResponse;
import com.example.volrabbit_be.persona.dto.PersonaDto;
import com.example.volrabbit_be.persona.service.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
@Tag(name = "Persona", description = "페르소나 관련 API (페르소나 조회, 페르소나 분석 조회)")
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    @Operation(summary = "전체 페르소나 목록 조회", description = "전체 페르소나 목록 조회 (id, 이름, 아이콘 URL)")
    public ApiResponse<List<PersonaDto.PersonaResponse>> getAllPersonas() {
        List<PersonaDto.PersonaResponse> data = personaService.getAllPersonas();

        return ApiResponse.success(data);
    }

    @GetMapping("/{personaId}")
    @Operation(summary = "특정 페르소나 조회", description = "특정 페르소나 조회 (id, 이름, 아이콘 URL)")
    public ApiResponse<PersonaDto.PersonaResponse> getPersonaByPersonaId(@PathVariable long personaId) {
        PersonaDto.PersonaResponse data = personaService.getPersonaByPersonaId(personaId);

        return ApiResponse.success(data);
    }

    @GetMapping("/{personaId}/analysis")
    @Operation(summary = "특정 페르소나 분석 조회", description = "해당 페르소나 id에 해당하는 페르소나 분석 조회")
    public ApiResponse<PersonaDto.PersonaAnalysisResponse> getPersonaAnalysisByPersonaId(@PathVariable long personaId) {
        PersonaDto.PersonaAnalysisResponse data = personaService.getPersonaAnalysisByPersonaId(personaId);

        return ApiResponse.success(data);
    }
}

package com.example.volrabbit_be.persona.service;

import com.example.volrabbit_be.persona.domain.Persona;
import com.example.volrabbit_be.persona.dto.PersonaDto;
import com.example.volrabbit_be.persona.repository.PersonaRepository;
import tools.jackson.databind.DatabindException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 페르소나 분석 파일(JSON)이 저장되어 있는 경로
    @Value("${volrabbit.persona.analysis.path:classpath:persona/analyses/}")
    Resource personaAnalysisJsonPath;

    /**
     * 모든 페르소나를 가져옴
     * @return 페르소나 응답 객체로 이루어진 리스트
     */
    public List<PersonaDto.PersonaResponse> getAllPersonas() {
        List<Persona> personas = personaRepository.findAll();

        List<PersonaDto.PersonaResponse> personasListResponse = personas.stream().map(persona -> {
            return PersonaDto.PersonaResponse.of(persona.getId(), persona.getName(), persona.getIconUrl());
        }).toList();

        return personasListResponse;
    }

    /**
     * 특정 페르소나를 가져옴
     * @param personaId 대상 페르소나 ID
     * @return 단일 페르소나 응답 객체
     */
    public PersonaDto.PersonaResponse getPersonaByPersonaId(long personaId) {
        Optional<Persona> optionalPersona = personaRepository.findById(personaId);

        Persona persona = optionalPersona.orElseThrow(() -> new IllegalArgumentException("해당 ID를 가진 페르소나가 없습니다."));

        return PersonaDto.PersonaResponse.of(persona.getId(), persona.getName(), persona.getIconUrl());
    }

    /**
     * 특정 페르소나의 페르소나 분석을 가져옴. 데이터는 JSON 파일로 저장되어 있음
     * @param personaId 대상 페르소나 ID
     * @return 단일 페르소나 분석 응답 객체
     */
    public PersonaDto.PersonaAnalysisResponse getPersonaAnalysisByPersonaId(long personaId) {
        try (InputStream is =
                     personaAnalysisJsonPath.createRelative(personaId + ".json").getInputStream()) {
            PersonaDto.PersonaAnalysisResponse analysisResponse =
                    objectMapper.readValue(is, PersonaDto.PersonaAnalysisResponse.class);

            return analysisResponse;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("해당 ID의 페르소나 분석 파일을 찾지 못했습니다.");
        } catch (DatabindException e) {
            throw new IllegalArgumentException("해당 ID의 페르소나 분석 파일이 잘못되었습니다.");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}

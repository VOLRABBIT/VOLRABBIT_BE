package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.global.ai.OpenAiClient;
import com.example.volrabbit_be.global.exception.ApiExceptions;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoStep;
import com.example.volrabbit_be.todo.domain.enumType.SegmentationType;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.todo.repository.TodoStepRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoSegmentationService {

    // 세분화 step content는 최대 30자
    private static final int STEP_MAX_LENGTH = 30;

    private final TodoRepository todoRepository;
    private final TodoStepRepository todoStepRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public void generateStepsIfNeeded(Long userId, Long todoId, SegmentationType segmentationType) {
        if (segmentationType == null || segmentationType == SegmentationType.NONE) return;

        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("투두가 존재하지 않습니다."));

        // 기존 단계 삭제 후 재생성
        todoStepRepository.deleteAllByTodoId(todo.getId());

        int targetCount = switch (segmentationType) {
            case TWO -> 2;
            case THREE -> 3;
            case AUTO -> -1; // 2~9개로 모델이 결정
            default -> -1;
        };

        String instructions =
                "너는 할 일을 작은 단계로 나누는 도우미야. " +
                        "각 단계는 실행 가능하고 구체적이어야 해. " +
                        "각 단계 content는 최대 " + STEP_MAX_LENGTH + "자 이하여야 해. " +
                        "출력은 반드시 JSON만 반환해.";

        String userInput =
                "할 일: " + todo.getContent() + "\n" +
                        (targetCount > 0
                                ? ("단계 개수: " + targetCount + "개로 고정\n")
                                : "단계 개수: 2~9개(한 자리 수)로 적절히\n") +
                        "각 단계 content는 최대 " + STEP_MAX_LENGTH + "자\n" +
                        "반환 형식: {\"steps\":[{\"content\":\"...\"}, ...]}";

        // ✅ 서버에서 order는 1..n으로 부여
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("steps"),
                "properties", Map.of(
                        "steps", Map.of(
                                "type", "array",
                                "minItems", (targetCount > 0 ? targetCount : 2),
                                "maxItems", (targetCount > 0 ? targetCount : 9),
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("content"),
                                        "properties", Map.of(
                                                // step content 길이 제한: 30자
                                                "content", Map.of(
                                                        "type", "string",
                                                        "minLength", 1,
                                                        "maxLength", STEP_MAX_LENGTH
                                                )
                                        )
                                )
                        )
                )
        );

        JsonNode result;
        try {
            result = openAiClient.callStructured(instructions, userInput, schema);
        } catch (Exception e) {
            log.error("AI segmentation call failed", e);
            throw new RuntimeException("세분화 AI 호출에 실패했습니다.", e);
        }

        JsonNode stepsNode = result.get("steps");
        if (stepsNode == null || !stepsNode.isArray()) {
            throw new ApiExceptions.BadRequest("세분화 결과 steps가 올바르지 않습니다.");
        }

        if (targetCount > 0 && stepsNode.size() != targetCount) {
            throw new ApiExceptions.BadRequest("세분화 결과 단계 수가 요청과 다릅니다.");
        }

        // content만 뽑아서 저장 (빈 값 방지 + 길이 제한 방어)
        List<String> contents = new ArrayList<>();
        for (JsonNode step : stepsNode) {
            JsonNode c = step.get("content");
            String content = (c == null) ? null : c.asText();
            if (content == null) continue;

            String trimmed = content.trim();
            if (trimmed.isBlank()) continue;

            if (trimmed.length() > STEP_MAX_LENGTH) {
                throw new ApiExceptions.BadRequest("세분화 단계 content는 최대 " + STEP_MAX_LENGTH + "자입니다.");
            }

            contents.add(trimmed);
        }

        if (targetCount > 0 && contents.size() != targetCount) {
            throw new ApiExceptions.BadRequest("세분화 결과 content가 비어있는 단계가 있습니다.");
        }

        if (targetCount <= 0 && contents.size() < 2) {
            throw new ApiExceptions.BadRequest("세분화 결과 단계가 너무 적습니다.");
        }

        long order = 1L;
        for (String content : contents) {
            TodoStep entity = TodoStep.builder()
                    .todo(todo)
                    .content(content)
                    .stepOrder(order)
                    .build();

            todoStepRepository.save(entity);
            order++;
        }
    }
}

package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.global.ai.OpenAiClient;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoStep;
import com.example.volrabbit_be.todo.domain.enumType.SegmentationType;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.todo.repository.TodoStepRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoSegmentationService {

    private final TodoRepository todoRepository;
    private final TodoStepRepository todoStepRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public void generateStepsIfNeeded(Long userId, Long todoId, SegmentationType segmentationType) {
        if (segmentationType == null || segmentationType == SegmentationType.NONE) return;

        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("투두가 존재하지 않습니다."));

        // 정책: 재생성 시 기존 steps 삭제 후 새로 생성
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
                        "출력은 반드시 JSON만 반환해.";

        String userInput =
                "할 일: " + todo.getContent() + "\n" +
                        (targetCount > 0 ? ("단계 개수: " + targetCount + "개로 고정\n") : "단계 개수: 2~9개(한 자리 수)로 적절히\n") +
                        "반환 형식: {\"steps\":[{\"order\":1,\"content\":\"...\"}, ...]}";

        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("steps"),
                "properties", Map.of(
                        "steps", Map.of(
                                "type", "array",
                                "minItems", 2,
                                "maxItems", 9,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("order", "content"),
                                        "properties", Map.of(
                                                "order", Map.of("type", "integer", "minimum", 1, "maximum", 9),
                                                "content", Map.of("type", "string", "minLength", 1, "maxLength", 60)
                                        )
                                )
                        )
                )
        );

        JsonNode result = openAiClient.callStructured(instructions, userInput, schema);
        JsonNode steps = result.get("steps");
        if (steps == null || !steps.isArray()) {
            throw new IllegalStateException("세분화 결과 steps가 올바르지 않습니다.");
        }

        if (targetCount > 0 && steps.size() != targetCount) {
            throw new IllegalStateException("세분화 결과 단계 수가 요청과 다릅니다.");
        }

        long order = 1L;
        for (JsonNode step : steps) {
            String content = step.get("content").asText();

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

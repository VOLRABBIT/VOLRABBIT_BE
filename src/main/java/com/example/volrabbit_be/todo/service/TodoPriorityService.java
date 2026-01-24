package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.global.ai.OpenAiClient;
import com.example.volrabbit_be.global.exception.ApiExceptions;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoPriority;
import com.example.volrabbit_be.todo.domain.enumType.PriorityQuadrant;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.repository.TodoPriorityRepository;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoPriorityService {

    private final TodoRepository todoRepository;
    private final TodoPriorityRepository todoPriorityRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public TodoDto.DailyPriorityResponse generate(Long userId, LocalDate date) {
        if (date == null) throw new ApiExceptions.BadRequest("date는 필수입니다.");

        List<Todo> todos = todoRepository.findAllByUserIdAndDueDateOrderByIdAsc(userId, date);

        // 덮어쓰기 정책
        todoPriorityRepository.deleteAllByTodoUserIdAndPriorityDate(userId, date);

        if (todos.isEmpty()) {
            return TodoDto.DailyPriorityResponse.builder()
                    .date(date)
                    .items(List.of())
                    .build();
        }

        // AI 입력
        StringBuilder sb = new StringBuilder();
        sb.append("date=").append(date).append("\n");
        sb.append("todos:\n");
        for (Todo t : todos) {
            sb.append("- todoId=").append(t.getId())
                    .append(", content=").append(t.getContent())
                    .append(", retry=").append(t.isRetryTodo())
                    .append("\n");
        }

        String instructions =
                "너는 할 일 우선순위를 정하는 도우미야. " +
                        "각 todo에 대해 quadrant(4분면)와 priorityOrder(0..n-1)를 정해. " +
                        "반드시 JSON만 반환해.";

        String userInput =
                sb +
                        "\n반환 형식: {\"items\":[{\"todoId\":1,\"quadrant\":\"URGENT_IMPORTANT\",\"priorityOrder\":0}, ...]}\n" +
                        "규칙: 입력에 있던 todoId만 포함하고, items 길이는 입력 todo 개수와 동일해야 하며, " +
                        "priorityOrder는 0..n-1을 중복 없이 모두 포함해.";

        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("items"),
                "properties", Map.of(
                        "items", Map.of(
                                "type", "array",
                                "minItems", todos.size(),
                                "maxItems", todos.size(),
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("todoId", "quadrant", "priorityOrder"),
                                        "properties", Map.of(
                                                "todoId", Map.of("type", "integer", "minimum", 1),
                                                "quadrant", Map.of(
                                                        "type", "string",
                                                        "enum", List.of(
                                                                "URGENT_IMPORTANT",
                                                                "URGENT_NOT_IMPORTANT",
                                                                "NOT_URGENT_IMPORTANT",
                                                                "NOT_URGENT_NOT_IMPORTANT"
                                                        )
                                                ),
                                                "priorityOrder", Map.of("type", "integer", "minimum", 0)
                                        )
                                )
                        )
                )
        );

        JsonNode result;
        try {
            result = openAiClient.callStructured(instructions, userInput, schema);
        } catch (Exception e) {
            log.error("AI priority call failed", e);
            return fallbackSaveAndReturn(todos, date);
        }

        JsonNode itemsNode = result.get("items");
        if (itemsNode == null || !itemsNode.isArray()) {
            // AI가 이상하게 주면 fallback
            log.error("AI priority result invalid: items missing");
            return fallbackSaveAndReturn(todos, date);
        }

        Map<Long, Todo> todoMap = new HashMap<>();
        for (Todo t : todos) todoMap.put(t.getId(), t);

        // 1) 파싱해서 todoId별로 1개만 남기기 (중복 방지)
        Map<Long, AiPriority> parsed = new HashMap<>();
        for (JsonNode it : itemsNode) {
            JsonNode todoIdNode = it.get("todoId");
            JsonNode quadNode = it.get("quadrant");
            JsonNode orderNode = it.get("priorityOrder");
            if (todoIdNode == null || quadNode == null || orderNode == null) continue;

            long todoId = todoIdNode.asLong();
            Todo todo = todoMap.get(todoId);
            if (todo == null) continue;

            PriorityQuadrant quadrant;
            try {
                quadrant = PriorityQuadrant.valueOf(quadNode.asText());
            } catch (Exception ex) {
                continue;
            }

            int order = orderNode.asInt();
            parsed.put(todoId, new AiPriority(todo, quadrant, order));
        }

        // 2) 누락된 todo는 뒤에 붙이고, order는 0..n-1로 재부여(중복/이상치 방지)
        List<AiPriority> list = new ArrayList<>(parsed.values());
        list.sort(Comparator.comparingInt(a -> a.orderHint));

        for (Todo t : todos) {
            if (!parsed.containsKey(t.getId())) {
                // 누락된 건 뒤로
                list.add(new AiPriority(t, PriorityQuadrant.NOT_URGENT_NOT_IMPORTANT, Integer.MAX_VALUE));
            }
        }

        List<TodoDto.PriorityItem> response = new ArrayList<>();
        int order = 0;
        for (AiPriority ap : list) {
            todoPriorityRepository.save(
                    TodoPriority.builder()
                            .todo(ap.todo)
                            .priorityDate(date)
                            .priorityQuadrant(ap.quadrant)
                            .priorityOrder(order)
                            .build()
            );

            response.add(TodoDto.PriorityItem.builder()
                    .todoId(ap.todo.getId())
                    .content(ap.todo.getContent())
                    .quadrant(ap.quadrant)
                    .quadrantLabel(ap.quadrant.label())
                    .priorityOrder(order)
                    .build());

            order++;
        }

        return TodoDto.DailyPriorityResponse.builder()
                .date(date)
                .items(response)
                .build();
    }

    public TodoDto.DailyPriorityResponse get(Long userId, LocalDate date) {
        if (date == null) throw new ApiExceptions.BadRequest("date는 필수입니다.");

        List<TodoPriority> rows = todoPriorityRepository
                .findAllByTodoUserIdAndPriorityDateOrderByPriorityOrderAsc(userId, date);

        List<TodoDto.PriorityItem> items = new ArrayList<>();
        for (TodoPriority row : rows) {
            PriorityQuadrant quadrant = row.getPriorityQuadrant();

            items.add(TodoDto.PriorityItem.builder()
                    .todoId(row.getTodo().getId())
                    .content(row.getTodo().getContent())
                    .quadrant(quadrant)
                    .quadrantLabel(quadrant.label())
                    .priorityOrder(row.getPriorityOrder())
                    .build());
        }

        return TodoDto.DailyPriorityResponse.builder()
                .date(date)
                .items(items)
                .build();
    }

    @Transactional
    public void reorder(Long userId, LocalDate date, TodoDto.PriorityReorderRequest request) {
        if (date == null) throw new ApiExceptions.BadRequest("date는 필수입니다.");
        if (request == null || request.getOrders() == null) return;

        for (TodoDto.PriorityReorderRequest.OrderPatch patch : request.getOrders()) {
            if (patch == null || patch.getTodoId() == null || patch.getPriorityOrder() == null) {
                throw new ApiExceptions.BadRequest("orders의 todoId/priorityOrder는 필수입니다.");
            }

            TodoPriority row = todoPriorityRepository.findByTodoIdAndPriorityDate(patch.getTodoId(), date)
                    .orElseThrow(() -> new ApiExceptions.NotFound("우선순위 데이터가 존재하지 않습니다. todoId=" + patch.getTodoId()));

            if (!row.getTodo().getUser().getId().equals(userId)) {
                throw new ApiExceptions.Unauthorized("권한이 없습니다.");
            }

            row.updateOrder(patch.getPriorityOrder());
        }
    }

    private TodoDto.DailyPriorityResponse fallbackSaveAndReturn(List<Todo> todos, LocalDate date) {
        List<TodoDto.PriorityItem> fallback = new ArrayList<>();
        int i = 0;
        for (Todo t : todos) {
            todoPriorityRepository.save(
                    TodoPriority.builder()
                            .todo(t)
                            .priorityDate(date)
                            .priorityQuadrant(PriorityQuadrant.NOT_URGENT_NOT_IMPORTANT)
                            .priorityOrder(i)
                            .build()
            );
            fallback.add(TodoDto.PriorityItem.builder()
                    .todoId(t.getId())
                    .content(t.getContent())
                    .quadrant(PriorityQuadrant.NOT_URGENT_NOT_IMPORTANT)
                    .quadrantLabel(PriorityQuadrant.NOT_URGENT_NOT_IMPORTANT.label())
                    .priorityOrder(i)
                    .build());
            i++;
        }
        return TodoDto.DailyPriorityResponse.builder().date(date).items(fallback).build();
    }

    private static class AiPriority {
        private final Todo todo;
        private final PriorityQuadrant quadrant;
        private final int orderHint;

        private AiPriority(Todo todo, PriorityQuadrant quadrant, int orderHint) {
            this.todo = todo;
            this.quadrant = quadrant;
            this.orderHint = orderHint;
        }
    }
}

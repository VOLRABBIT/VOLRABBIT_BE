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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoPriorityService {

    private final TodoRepository todoRepository;
    private final TodoPriorityRepository todoPriorityRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public TodoDto.DailyPriorityResponse generate(Long userId, LocalDate date) {
        if (date == null) {
            throw new ApiExceptions.BadRequest("date는 필수입니다.");
        }
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
                        "규칙: 입력에 있던 todoId만 포함하고 priorityOrder는 0..n-1로 중복 없이.";

        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("items"),
                "properties", Map.of(
                        "items", Map.of(
                                "type", "array",
                                "minItems", 1,
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
            // AI 실패 fallback: id 순서대로 NOT_URGENT_NOT_IMPORTANT
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

        JsonNode items = result.get("items");
        if (items == null || !items.isArray()) {
            throw new RuntimeException("우선순위 결과 items가 올바르지 않습니다.");
        }

        Map<Long, Todo> todoMap = new HashMap<>();
        for (Todo t : todos) todoMap.put(t.getId(), t);

        List<TodoDto.PriorityItem> response = new ArrayList<>();

        for (JsonNode it : items) {
            long todoId = it.get("todoId").asLong();
            String quadrantStr = it.get("quadrant").asText();
            int order = it.get("priorityOrder").asInt();

            Todo todo = todoMap.get(todoId);
            if (todo == null) continue;

            PriorityQuadrant quadrant;
            try {
                quadrant = PriorityQuadrant.valueOf(quadrantStr);
            } catch (Exception ex) {
                throw new RuntimeException("우선순위 결과 quadrant 값이 올바르지 않습니다.");
            }

            todoPriorityRepository.save(
                    TodoPriority.builder()
                            .todo(todo)
                            .priorityDate(date)
                            .priorityQuadrant(quadrant)
                            .priorityOrder(order)
                            .build()
            );

            response.add(TodoDto.PriorityItem.builder()
                    .todoId(todo.getId())
                    .content(todo.getContent())
                    .quadrant(quadrant)
                    .quadrantLabel(quadrant.label())
                    .priorityOrder(order)
                    .build());
        }

        response.sort(Comparator.comparingInt(TodoDto.PriorityItem::getPriorityOrder));

        return TodoDto.DailyPriorityResponse.builder()
                .date(date)
                .items(response)
                .build();
    }

    public TodoDto.DailyPriorityResponse get(Long userId, LocalDate date) {
        if (date == null) {
            throw new ApiExceptions.BadRequest("date는 필수입니다.");
        }

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
}

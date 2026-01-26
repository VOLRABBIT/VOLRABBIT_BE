package com.example.volrabbit_be.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final OpenAiProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode callStructured(String instructions, String userInput, Map<String, Object> jsonSchema) {
        String url = "https://api.openai.com/v1/responses";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(props.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> input = List.of(
                Map.of(
                        "role", "system",
                        "content", List.of(Map.of("type", "input_text", "text", instructions))
                ),
                Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "input_text", "text", userInput))
                )
        );

        Map<String, Object> textFormat = new HashMap<>();
        textFormat.put("type", "json_schema");
        textFormat.put("name", "structured_output");
        textFormat.put("strict", true);
        textFormat.put("schema", jsonSchema);

        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("input", input);
        body.put("text", Map.of("format", textFormat));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (HttpStatusCodeException e) {
            log.error("OpenAI API error. status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("OpenAI call failed (network/unknown)", e);
            throw e;
        }

        String raw = response.getBody();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("OpenAI 응답 바디가 비었습니다.");
        }

        try {
            JsonNode root = objectMapper.readTree(raw);

            // Responses API: output[0].content[0]에서 output_text를 찾아서 text를 꺼내자
            JsonNode output = root.get("output");
            if (output == null || !output.isArray() || output.isEmpty()) {
                log.error("Unexpected OpenAI response (no output). raw={}", raw);
                throw new IllegalStateException("OpenAI 응답 output이 비었습니다.");
            }

            JsonNode contentArr = output.get(0).get("content");
            if (contentArr == null || !contentArr.isArray() || contentArr.isEmpty()) {
                log.error("Unexpected OpenAI response (no content). raw={}", raw);
                throw new IllegalStateException("OpenAI 응답 content가 비었습니다.");
            }

            // content 배열에서 type이 output_text인 항목을 찾음
            JsonNode outputTextNode = null;
            for (JsonNode c : contentArr) {
                JsonNode typeNode = c.get("type");
                if (typeNode != null && "output_text".equals(typeNode.asText())) {
                    outputTextNode = c;
                    break;
                }
            }
            if (outputTextNode == null) {
                // 혹시 첫 원소에 바로 text가 있는 케이스도 커버
                outputTextNode = contentArr.get(0);
            }

            JsonNode textNode = outputTextNode.get("text");
            if (textNode == null || textNode.asText().isBlank()) {
                log.error("Unexpected OpenAI response (no output text). raw={}", raw);
                throw new IllegalStateException("OpenAI 응답 text가 비었습니다.");
            }

            // 모델이 준 JSON 문자열을 실제 JsonNode로 파싱
            return objectMapper.readTree(textNode.asText());

        } catch (Exception e) {
            log.error("OpenAI response parse failed. raw={}", raw, e);
            throw new IllegalStateException("OpenAI 응답 파싱 실패", e);
        }
    }
}

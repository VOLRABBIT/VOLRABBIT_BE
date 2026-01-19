package com.example.volrabbit_be.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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

        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "instructions", instructions,
                "input", userInput,
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "strict", true,
                                "schema", jsonSchema
                        )
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode output = root.get("output");
            if (output == null || !output.isArray() || output.isEmpty()) {
                throw new IllegalStateException("OpenAI 응답 output이 비었습니다.");
            }

            JsonNode contentArr = output.get(0).get("content");
            if (contentArr == null || !contentArr.isArray() || contentArr.isEmpty()) {
                throw new IllegalStateException("OpenAI 응답 content가 비었습니다.");
            }

            String jsonText = contentArr.get(0).get("text").asText();
            return objectMapper.readTree(jsonText);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 응답 파싱 실패", e);
        }
    }
}

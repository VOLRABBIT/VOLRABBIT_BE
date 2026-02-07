package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.service.TodoNfcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Todo-NFC", description = "NFC 인증 API")
public class TodoNfcController {

    private final TodoNfcService todoNfcService;

    @Operation(summary = "NFC 태그 등록", description = "TAG 인증 투두에 대해 tagCode를 발급하고, 태그에 기록할 URL(ndefUrl)을 반환합니다.")
    @PostMapping("/api/todos/{todoId}/nfc/register")
    public ResponseEntity<TodoDto.NfcRegisterResponse> register(
            @PathVariable Long todoId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoNfcService.register(userId, todoId));
    }

    @Operation(summary = "NFC 인증", description = "앱이 tagCode를 전송하면, 해당 투두를 인증 완료 처리합니다.")
    @PostMapping("/api/todos/nfc/verify")
    public ResponseEntity<TodoDto.VerificationResponse> verify(
            @RequestBody TodoDto.NfcVerifyRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoNfcService.verify(userId, request));
    }
}

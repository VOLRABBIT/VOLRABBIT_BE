package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    // 생성
    @PostMapping
    public ResponseEntity<TodoDto.CreateResponse> create(@RequestBody TodoDto.CreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.create(userId, request));
    }

    // 전체 할 일 조회
    @GetMapping
    public ResponseEntity<List<TodoDto.SummaryResponse>> getAll() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.getAll(userId));
    }

    // 날짜별 조회
    @GetMapping(params = "date")
    public ResponseEntity<List<TodoDto.SummaryResponse>> getByDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.getByDate(userId, date));
    }

    // 단건 조회
    @GetMapping("/{todoId}")
    public ResponseEntity<TodoDto.DetailResponse> getOne(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.getOne(userId, todoId));
    }

    // 수정
    @PatchMapping("/{todoId}")
    public ResponseEntity<TodoDto.UpdateResponse> update(
            @PathVariable Long todoId,
            @RequestBody TodoDto.UpdateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.update(userId, todoId, request));
    }

    // 성공 (혹시 몰라서 상태 값 변화 구현만 해둠)
    @PatchMapping("/{todoId}/complete")
    public ResponseEntity<TodoDto.StatusResponse> complete(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.complete(userId, todoId));
    }

    // 실패
    @PatchMapping("/{todoId}/fail")
    public ResponseEntity<TodoDto.StatusResponse> fail(
            @PathVariable Long todoId,
            @RequestBody(required = false) TodoDto.OutcomeRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.fail(userId, todoId, request));
    }

    // 포기
    @PatchMapping("/{todoId}/give-up")
    public ResponseEntity<TodoDto.StatusResponse> giveUp(
            @PathVariable Long todoId,
            @RequestBody TodoDto.OutcomeRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.giveUp(userId, todoId, request));
    }

    // 재도전
    @PostMapping("/{todoId}/retry")
    public ResponseEntity<TodoDto.RetryResponse> retry(
            @PathVariable Long todoId,
            @RequestBody TodoDto.RetryRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.retry(userId, todoId, request));
    }

    // 포기 사유 입력 후 재도전
    @PostMapping("/{todoId}/give-up/retry")
    public ResponseEntity<TodoDto.RetryResponse> giveUpAndRetry(
            @PathVariable Long todoId,
            @RequestBody TodoDto.GiveUpAndRetryRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.giveUpAndRetry(userId, todoId, request));
    }

    // 자가 체크
    @PostMapping("/{todoId}/check")
    public ResponseEntity<TodoDto.VerificationResponse> verifyCheck(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.verifyCheck(userId, todoId));
    }

    //체크 인증 취소(미완료로 되돌리기) - CHECK 선택한 투두만 가능
    @DeleteMapping("/{todoId}/check")
    public ResponseEntity<TodoDto.StatusResponse> unverifyCheck(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.unverifyCheck(userId, todoId));
    }

    // 사진 인증
    @PostMapping("/{todoId}/photo-proof")
    public ResponseEntity<TodoDto.VerificationResponse> verifyPhotoProof(
            @PathVariable Long todoId,
            @RequestBody TodoDto.PhotoProofRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.verifyPhotoProof(userId, todoId, request));
    }

    // NFC
    @PostMapping("/{todoId}/tag")
    public ResponseEntity<TodoDto.VerificationResponse> verifyTag(
            @PathVariable Long todoId,
            @RequestBody TodoDto.TagRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.verifyTag(userId, todoId, request));
    }
}

package com.example.volrabbit_be.todo.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
@Tag(name = "Todo", description = "투두 관련 API (생성/조회/수정/상태 값 변화/인증)")
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "투두 생성", description = "Access Token 인증 후, 생성 폼 안의 내용을 빠짐없이 작성하면 생성됩니다.")
    @PostMapping
    public ResponseEntity<TodoDto.CreateResponse> create(@RequestBody TodoDto.CreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.create(userId, request));
    }

    @Operation(summary = "투두 전체 목록 조회", description = "유저가 생성한 전체 투두 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<TodoDto.SummaryResponse>> getAll() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.getAll(userId));
    }

    @Operation(summary = "날짜별 투두 조회", description = "유저가 선택한 날을 쿼리 파라미터로 받아 해당 날에 생성된 투두를 전체 조회합니다.")
    @GetMapping(params = "date")
    public ResponseEntity<List<TodoDto.SummaryResponse>> getByDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.getByDate(userId, date));
    }

    @Operation(summary = "투두 단건 조회", description = "경로 변수로 받은 todoId를 통해 해당하는 투두를 조회합니다.")
    @GetMapping("/{todoId}")
    public ResponseEntity<TodoDto.DetailResponse> getOne(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.getOne(userId, todoId));
    }

    @Operation(summary = "투두 수정", description = "경로 변수로 받은 todoId로 이전에 작성한 투두가 불러와지며 수정 가능합니다.")
    @PatchMapping("/{todoId}")
    public ResponseEntity<TodoDto.DetailResponse> update(
            @PathVariable Long todoId,
            @RequestBody TodoDto.UpdateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.update(userId, todoId, request));
    }

    @Operation(
            summary = "상태 값 변화(complete)",
            description = "(인증 API가 있어서 아마 사용되진 않을 것 같은데, 혹시 몰라서 구현해뒀습니다.)" +
                    "투두 생성시 선택했던 방식으로 인증이 완료됐다면 " +
                    "해당 투두는 complete로 상태 값이 수정됩니다."
    )
    @PatchMapping("/{todoId}/complete")
    public ResponseEntity<TodoDto.StatusResponse> complete(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.complete(userId, todoId));
    }

    @Operation(
            summary = "상태 값 변화(fail)",
            description = "유저가 투두 포기 후 재도전하지 않고, " +
                    "실패 원인을 제출했을 때 " +
                    "상태 값 fail 처리를 담당하는 API 입니다."
    )
    @PatchMapping("/{todoId}/fail")
    public ResponseEntity<TodoDto.StatusResponse> fail(
            @PathVariable Long todoId,
            @RequestBody(required = false) TodoDto.OutcomeRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.fail(userId, todoId, request));
    }

    @Operation(
            summary = "상태 값 변화(give_up)",
            description = "유저가 포기 원인을 제출했을 때 상태 값 give_up 처리를 담당하는 API 입니다."
    )
    @PatchMapping("/{todoId}/give-up")
    public ResponseEntity<TodoDto.StatusResponse> giveUp(
            @PathVariable Long todoId,
            @RequestBody TodoDto.OutcomeRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.giveUp(userId, todoId, request));
    }

    @Operation(summary = "상태 값 변화(retry)", description = "(아마 사용되지 않을 것 같습니다.) 상태 값을 retry로 바꾸는 API 입니다.")
    @PostMapping("/{todoId}/retry")
    public ResponseEntity<TodoDto.RetryResponse> retry(
            @PathVariable Long todoId,
            @RequestBody TodoDto.RetryRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.retry(userId, todoId, request));
    }

    // 포기 사유 입력 후 재도전
    @Operation(
            summary = "재도전 투두 생성",
            description = "기존 투두는 give_up 처리하고, " +
                    "동일한 내용의 inprogress 투두를 재생성합니다. " +
                    "이때 기존 todoId는 새로운 투두의 retryFromTodoId 값에 들어갑니다."
    )
    @PostMapping("/{todoId}/give-up/retry")
    public ResponseEntity<TodoDto.RetryResponse> giveUpAndRetry(
            @PathVariable Long todoId,
            @RequestBody TodoDto.GiveUpAndRetryRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.giveUpAndRetry(userId, todoId, request));
    }

    // 자가 체크
    @Operation(summary = "자가 체크 인증", description = "")
    @PostMapping("/{todoId}/check")
    public ResponseEntity<TodoDto.VerificationResponse> verifyCheck(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.verifyCheck(userId, todoId));
    }

    //체크 인증 취소(미완료로 되돌리기) - CHECK 선택한 투두만 가능
    @Operation(summary = "자가 체크 취소", description = "자가 체크로 complete 됐던 투두를 미완료 상태로 되돌립니다. (todoId)")
    @DeleteMapping("/{todoId}/check")
    public ResponseEntity<TodoDto.StatusResponse> unverifyCheck(@PathVariable Long todoId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(todoService.unverifyCheck(userId, todoId));
    }

    // 사진 인증
    @Operation(
            summary = "사진 업로드 인증",
            description = "사진 파일을 업로드하여 해당 투두를 complete 상태로 변경합니다. (multipart/form-data, key=photo)"
    )
    @PostMapping(
            value = "/{todoId}/photo-proof",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TodoDto.VerificationResponse> verifyPhotoProof(
            @PathVariable Long todoId,
            @RequestParam("photo") MultipartFile photo
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ResponseEntity.ok(
                todoService.verifyPhotoProof(userId, todoId, photo)
        );
    }

}

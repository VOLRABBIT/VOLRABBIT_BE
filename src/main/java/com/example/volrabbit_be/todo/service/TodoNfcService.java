package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.global.exception.ApiExceptions;
import com.example.volrabbit_be.todo.domain.NfcTag;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoVerification;
import com.example.volrabbit_be.todo.domain.enumType.VerificationType;
import com.example.volrabbit_be.todo.domain.enumType.todoStatus;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.repository.NfcTagRepository;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.todo.repository.TodoVerificationRepository;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoNfcService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final TodoVerificationRepository todoVerificationRepository;
    private final NfcTagRepository nfcTagRepository;

    /**
     * 태그에 쓸 HTTPS URL base
     * - 로컬: http://localhost:8080
     * - 운영: https://yourapp.com
     */
    @Value("${app.public-base-url:https://yourapp.com}")
    private String publicBaseUrl;

    /**
     * NFC 등록: tagCode 발급 + DB 저장 + ndefUrl 반환
     * - todo 1개당 nfcTag 1개만 등록 가능 (A 방식)
     * - 실제 태그에는 ndefUrl을 기록하면 됨
     */
    @Transactional
    public TodoDto.NfcRegisterResponse register(Long userId, Long todoId) {
        Todo todo = findTodo(todoId, userId);
        validateRegisterable(todo);

        if (nfcTagRepository.existsByTodoId(todoId)) {
            throw new ApiExceptions.Conflict("이미 NFC 태그가 등록된 투두입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("사용자가 존재하지 않습니다."));

        String tagCode = issueUniqueTagCode();

        // 투두-태그 매핑 저장 (등록 정보)
        nfcTagRepository.save(NfcTag.create(todo, user, tagCode));

        // 태그에 기록될 URL (NDEF payload)
        String ndefUrl = normalizeBaseUrl(publicBaseUrl) + "/nfc/tags/" + tagCode;

        return TodoDto.NfcRegisterResponse.builder()
                .todoId(todoId)
                .tagCode(tagCode)
                .ndefUrl(ndefUrl)
                .build();
    }

    /**
     * NFC 인증: 앱이 tagCode를 보내면 tagCode로 todo 찾아 complete 처리
     * - 물리 태그가 없어도, register 응답의 tagCode로 verify를 호출하면 로직 테스트 가능
     */
    @Transactional
    public TodoDto.VerificationResponse verify(Long userId, TodoDto.NfcVerifyRequest request) {
        if (request == null || request.getTagCode() == null || request.getTagCode().isBlank()) {
            throw new ApiExceptions.BadRequest("tagCode는 필수입니다.");
        }

        NfcTag tag = nfcTagRepository.findByTagCode(request.getTagCode())
                .orElseThrow(() -> new ApiExceptions.NotFound("등록되지 않은 NFC 태그입니다."));

        if (!tag.isActive()) {
            throw new ApiExceptions.Conflict("폐기된 NFC 태그입니다.");
        }

        // 남의 태그 인증 방지
        if (!tag.getUser().getId().equals(userId)) {
            throw new ApiExceptions.Forbidden("본인의 NFC 태그만 사용할 수 있습니다.");
        }

        Todo todo = tag.getTodo();

        // 투두 상태/인증 방식/중복 인증 검증
        validateVerifiable(todo, VerificationType.TAG);

        // 인증 완료 기록(로그) 저장
        TodoVerification verification = TodoVerification.builder()
                .todo(todo)
                .build();

        verification.verifyByNfc();

        TodoVerification saved = todoVerificationRepository.save(verification);

        // 투두 완료 처리
        todo.complete();

        return TodoDto.VerificationResponse.builder()
                .todoId(todo.getId())
                .todoStatus(todo.getTodoStatus())
                .verifiedAt(saved.getCreatedDate())
                .build();
    }

    // ===== private helpers =====

    private String issueUniqueTagCode() {
        for (int i = 0; i < 3; i++) {
            String code = UUID.randomUUID().toString();
            if (!nfcTagRepository.existsByTagCode(code)) return code;
        }
        throw new ApiExceptions.BadRequest("NFC tagCode 발급에 실패했습니다. 다시 시도해주세요.");
    }

    private Todo findTodo(Long todoId, Long userId) {
        return todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("투두가 존재하지 않습니다."));
    }

    private void validateRegisterable(Todo todo) {
        if (todo.getTodoStatus() != todoStatus.IN_PROGRESS) {
            throw new ApiExceptions.Conflict("진행 중인 투두만 NFC 태그를 등록할 수 있습니다.");
        }
        if (todo.getVerificationType() != VerificationType.TAG) {
            throw new ApiExceptions.BadRequest("NFC(TAG) 인증 투두만 NFC 태그를 등록할 수 있습니다.");
        }
        if (todoVerificationRepository.existsByTodoId(todo.getId())) {
            throw new ApiExceptions.Conflict("이미 인증 처리된 투두입니다.");
        }
    }

    private void validateVerifiable(Todo todo, VerificationType expectedType) {
        if (todo.getTodoStatus() != todoStatus.IN_PROGRESS) {
            throw new ApiExceptions.Conflict("진행 중인 투두만 인증할 수 있습니다.");
        }
        if (todo.getVerificationType() != expectedType) {
            throw new ApiExceptions.BadRequest("해당 투두의 인증 방식과 요청 API가 일치하지 않습니다.");
        }
        if (todoVerificationRepository.existsByTodoId(todo.getId())) {
            throw new ApiExceptions.Conflict("이미 인증 처리된 투두입니다.");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return "https://yourapp.com";
        if (baseUrl.endsWith("/")) return baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl;
    }
}

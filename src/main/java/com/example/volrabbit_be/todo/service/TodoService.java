package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.todo.domain.Category;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.TodoOutcome;
import com.example.volrabbit_be.todo.domain.TodoVerification;
import com.example.volrabbit_be.todo.domain.enumType.*;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.todo.repository.CategoryRepository;
import com.example.volrabbit_be.todo.repository.TodoOutcomeRepository;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.todo.repository.TodoVerificationRepository;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TodoOutcomeRepository todoOutcomeRepository;
    private final TodoVerificationRepository todoVerificationRepository;

    @Transactional
    public TodoDto.CreateResponse create(Long userId, TodoDto.CreateRequest request) {
        validateCreateOrUpdateRequest(
                request.getCategoryId(),
                request.getContent(),
                request.getDueType(),
                request.getRepeatType(),
                request.getSegmentationType(),
                request.getVerificationType()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));

        if (!category.isActive()) {
            throw new IllegalArgumentException("비활성화된 카테고리는 선택할 수 없습니다.");
        }

        LocalDate dueDate = resolveDueDate(request.getDueType(), request.getDueDate());

        Todo todo = Todo.builder()
                .user(user)
                .category(category)
                .content(request.getContent())
                .dueType(request.getDueType())
                .dueDate(dueDate)
                .repeatType(request.getRepeatType())
                .segmentationType(request.getSegmentationType())
                .verificationType(request.getVerificationType())
                .build();

        Todo saved = todoRepository.save(todo);

        return TodoDto.CreateResponse.builder()
                .todoId(saved.getId())
                .build();
    }

    public List<TodoDto.SummaryResponse> getAll(Long userId) {
        return todoRepository.findAllByUserIdOrderByDueDateAscIdAsc(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public List<TodoDto.SummaryResponse> getByDate(Long userId, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("조회 날짜가 올바르지 않습니다.");
        }

        return todoRepository.findAllByUserIdAndDueDateOrderByIdAsc(userId, date)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public TodoDto.DetailResponse getOne(Long userId, Long todoId) {
        Todo todo = findTodo(todoId, userId);
        return toDetail(todo);
    }

    @Transactional
    public TodoDto.UpdateResponse update(Long userId, Long todoId, TodoDto.UpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 바디가 필요합니다.");
        }

        Todo todo = findTodo(todoId, userId);

        // 1) content (옵션)
        if (request.getContent() != null) {
            String content = request.getContent();
            if (content.isBlank()) throw new IllegalArgumentException("content는 비어 있을 수 없습니다.");
            if (content.length() > 10) throw new IllegalArgumentException("content는 최대 10자입니다.");
        }

        // 2) category (옵션)
        Category newCategory = null;
        if (request.getCategoryId() != null) {
            newCategory = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));

            if (!newCategory.isActive()) {
                throw new IllegalArgumentException("비활성화된 카테고리는 선택할 수 없습니다.");
            }
        }

        // 3) dueType / dueDate 규칙
        // - dueDate 단독 변경은 불허 (CUSTOM일 때만 의미가 있으니 혼란 방지)
        // - dueType이 바뀌면 dueDate도 그에 맞게 자동 재계산
        // - dueType=CUSTOM이면 dueDate 필수
        DueType newDueType = null;
        LocalDate newDueDate = null;

        if (request.getDueType() == null && request.getDueDate() != null) {
            throw new IllegalArgumentException("dueDate만 단독으로 수정할 수 없습니다. dueType과 함께 보내주세요.");
        }

        if (request.getDueType() != null) {
            newDueType = request.getDueType();

            if (newDueType == DueType.CUSTOM) {
                if (request.getDueDate() == null) {
                    throw new IllegalArgumentException("dueType이 CUSTOM이면 dueDate는 필수입니다.");
                }
                newDueDate = request.getDueDate();
            } else {
                // TODAY / TOMORROW는 서버가 날짜를 계산해줌
                newDueDate = resolveDueDate(newDueType, null);
            }
        }

        // 4) 나머지 enum들 (옵션 + null이면 변경 없음)
        if (request.getRepeatType() == null
                && request.getSegmentationType() == null
                && request.getVerificationType() == null
                && request.getCategoryId() == null
                && request.getContent() == null
                && request.getDueType() == null
                && request.getDueDate() == null) {
            // 진짜 아무것도 안 보낸 케이스를 막고 싶으면 활성화
            // throw new IllegalArgumentException("수정할 값이 없습니다.");
        }

        // 5) patch 적용
        todo.patchUpdate(
                newCategory,
                request.getContent(),
                newDueDate,
                newDueType,
                request.getRepeatType(),
                request.getSegmentationType(),
                request.getVerificationType()
        );

        return TodoDto.UpdateResponse.builder()
                .todoId(todo.getId())
                .build();
    }


    @Transactional
    public TodoDto.StatusResponse complete(Long userId, Long todoId) {
        Todo todo = findTodo(todoId, userId);
        todo.complete();

        return TodoDto.StatusResponse.builder()
                .todoId(todo.getId())
                .todoStatus(todo.getTodoStatus())
                .build();
    }

    /**
     * - 일반 투두 FAIL: 사유 필수 + TodoOutcome 저장
     * - 재도전 투두 FAIL: 사유 저장 X (request 없어도 OK)
     */
    @Transactional
    public TodoDto.StatusResponse fail(Long userId, Long todoId, TodoDto.OutcomeRequest request) {
        Todo todo = findTodo(todoId, userId);
        todo.fail();

        // 재도전 투두는 사유 저장 안 함
        if (!todo.isRetryTodo()) {
            validateOutcomeRequest(request);

            TodoOutcome outcome = TodoOutcome.builder()
                    .todo(todo)
                    .outcomeStatus(OutcomeStatus.FAIL)
                    .reasonCode(request.getReasonCode())
                    .customText(request.getCustomText())
                    .build();

            todoOutcomeRepository.save(outcome);
        }

        return TodoDto.StatusResponse.builder()
                .todoId(todo.getId())
                .todoStatus(todo.getTodoStatus())
                .build();
    }

    @Transactional
    public TodoDto.StatusResponse giveUp(Long userId, Long todoId, TodoDto.OutcomeRequest request) {
        validateOutcomeRequest(request);

        Todo todo = findTodo(todoId, userId);
        todo.giveUp(); // 엔티티에서 IN_PROGRESS만 허용

        TodoOutcome outcome = TodoOutcome.builder()
                .todo(todo)
                .outcomeStatus(OutcomeStatus.GIVE_UP)
                .reasonCode(request.getReasonCode())
                .customText(request.getCustomText())
                .build();

        todoOutcomeRepository.save(outcome);

        return TodoDto.StatusResponse.builder()
                .todoId(todo.getId())
                .todoStatus(todo.getTodoStatus())
                .build();
    }

    /**
     * 포기 + 재도전 (A안)
     * - 포기 사유는 GIVE_UP으로 저장
     * - 기존 투두 상태는 GIVE_UP으로 변경
     * - 동일 내용의 새 투두를 IN_PROGRESS로 생성 (날짜 선택)
     */
    @Transactional
    public TodoDto.RetryResponse giveUpAndRetry(Long userId, Long todoId, TodoDto.GiveUpAndRetryRequest request) {
        validateOutcomeRequest(request);

        Todo original = findTodo(todoId, userId);

        // 1) 포기 상태로 변경 (IN_PROGRESS만 가능)
        original.giveUp();

        // 2) 포기 사유 저장
        TodoOutcome outcome = TodoOutcome.builder()
                .todo(original)
                .outcomeStatus(OutcomeStatus.GIVE_UP)
                .reasonCode(request.getReasonCode())
                .customText(request.getCustomText())
                .build();
        todoOutcomeRepository.save(outcome);

        // 3) 새 투두 생성 (사용자 날짜 선택)
        DueType newDueType = request.getDueType();
        if (newDueType != DueType.TOMORROW && newDueType != DueType.CUSTOM) {
            throw new IllegalArgumentException("재도전 dueType은 TOMORROW 또는 CUSTOM만 가능합니다.");
        }

        LocalDate newDueDate = resolveRetryDueDate(newDueType, request.getDueDate());

        Todo newTodo = original.createRetryTodo(newDueDate, newDueType);
        Todo saved = todoRepository.save(newTodo);

        return TodoDto.RetryResponse.builder()
                .originalTodoId(original.getId())
                .originalTodoStatus(original.getTodoStatus()) // GIVE_UP
                .newTodoId(saved.getId())
                .newDueType(saved.getDueType())
                .newDueDate(saved.getDueDate())
                .build();
    }

    /**
     * 재도전: previousStatus 없이 original 상태 기반으로 처리
     * - 원본이 FAIL/GIVE_UP일 때만 가능
     */
    @Transactional
    public TodoDto.RetryResponse retry(Long userId, Long todoId, TodoDto.RetryRequest request) {
        Todo original = findTodo(todoId, userId);

        if (original.getTodoStatus() != todoStatus.FAIL && original.getTodoStatus() != todoStatus.GIVE_UP) {
            throw new IllegalStateException("실패/포기한 투두만 재도전할 수 있습니다.");
        }

        DueType newDueType = request.getDueType();
        if (newDueType != DueType.TOMORROW && newDueType != DueType.CUSTOM) {
            throw new IllegalArgumentException("재도전 dueType은 TOMORROW 또는 CUSTOM만 가능합니다.");
        }

        LocalDate newDueDate = resolveRetryDueDate(newDueType, request.getDueDate());

        Todo newTodo = original.createRetryTodo(newDueDate, newDueType);
        Todo saved = todoRepository.save(newTodo);

        return TodoDto.RetryResponse.builder()
                .originalTodoId(original.getId())
                .originalTodoStatus(original.getTodoStatus())
                .newTodoId(saved.getId())
                .newDueType(saved.getDueType())
                .newDueDate(saved.getDueDate())
                .build();
    }

    /**
     * 자정 지나도 못한 투두 자동 FAIL 처리
     * - 사유 저장 X
     * - dueDate < today && IN_PROGRESS 인 것들만
     */
    @Transactional
    public int autoFailExpiredTodos(LocalDate today) {
        List<Todo> targets = todoRepository.findAllByDueDateBeforeAndTodoStatus(today, todoStatus.IN_PROGRESS);

        for (Todo todo : targets) {
            todo.fail();
        }

        return targets.size();
    }

    // ===== 인증 처리 =====

    @Transactional
    public TodoDto.VerificationResponse verifyCheck(Long userId, Long todoId) {
        Todo todo = findTodo(todoId, userId);
        validateVerifiable(todo, VerificationType.CHECK);

        TodoVerification verification = TodoVerification.builder()
                .todo(todo)
                .build();
        verification.verifyByCheck();

        TodoVerification saved = todoVerificationRepository.save(verification);
        todo.complete();

        return toVerificationResponse(todo, saved.getCreatedDate());
    }

    @Transactional
    public TodoDto.StatusResponse unverifyCheck(Long userId, Long todoId) {
        Todo todo = findTodo(todoId, userId);

        if (todo.getVerificationType() != VerificationType.CHECK) {
            throw new IllegalArgumentException("체크 인증 투두만 체크 해제가 가능합니다.");
        }

        if (todo.getTodoStatus() != todoStatus.SUCCESS) {
            throw new IllegalStateException("완료 표시됐던 투두만 체크 해제할 수 있습니다.");
        }

        todoVerificationRepository.findByTodoId(todo.getId())
                .orElseThrow(() -> new IllegalStateException("체크 인증 기록이 없습니다."));

        todoVerificationRepository.deleteByTodoId(todo.getId());
        todo.reopenToInProgress();

        return TodoDto.StatusResponse.builder()
                .todoId(todo.getId())
                .todoStatus(todo.getTodoStatus())
                .build();
    }

    @Transactional
    public TodoDto.VerificationResponse verifyPhotoProof(Long userId, Long todoId, TodoDto.PhotoProofRequest request) {
        if (request == null || request.getPhotoUrl() == null || request.getPhotoUrl().isBlank()) {
            throw new IllegalArgumentException("photoUrl은 필수입니다.");
        }

        Todo todo = findTodo(todoId, userId);
        validateVerifiable(todo, VerificationType.PHOTO_PROOF);

        TodoVerification verification = TodoVerification.builder()
                .todo(todo)
                .build();
        verification.verifyByPhoto(request.getPhotoUrl());

        TodoVerification saved = todoVerificationRepository.save(verification);
        todo.complete();

        return toVerificationResponse(todo, saved.getCreatedDate());
    }

    @Transactional
    public TodoDto.VerificationResponse verifyTag(Long userId, Long todoId, TodoDto.TagRequest request) {
        if (request == null || request.getTagText() == null || request.getTagText().isBlank()) {
            throw new IllegalArgumentException("tagText는 필수입니다.");
        }

        Todo todo = findTodo(todoId, userId);
        validateVerifiable(todo, VerificationType.TAG);

        TodoVerification verification = TodoVerification.builder()
                .todo(todo)
                .build();
        verification.verifyByTag(request.getTagText());

        TodoVerification saved = todoVerificationRepository.save(verification);
        todo.complete();

        return toVerificationResponse(todo, saved.getCreatedDate());
    }

    // ===== private helpers =====

    private void validateCreateOrUpdateRequest(
            Long categoryId,
            String content,
            DueType dueType,
            RepeatType repeatType,
            SegmentationType segmentationType,
            VerificationType verificationType
    ) {
        if (categoryId == null) throw new IllegalArgumentException("categoryId는 필수입니다.");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("content는 필수입니다.");
        if (content.length() > 10) throw new IllegalArgumentException("content는 최대 10자입니다.");
        if (dueType == null) throw new IllegalArgumentException("dueType은 필수입니다.");
        if (repeatType == null) throw new IllegalArgumentException("repeatType은 필수입니다.");
        if (segmentationType == null) throw new IllegalArgumentException("segmentationType은 필수입니다.");
        if (verificationType == null) throw new IllegalArgumentException("verificationType은 필수입니다.");
    }

    // OutcomeRequest에도 ETC 검증 추가
    private void validateOutcomeRequest(TodoDto.OutcomeRequest request) {
        if (request == null) throw new IllegalArgumentException("사유 입력이 필요합니다.");
        if (request.getReasonCode() == null) throw new IllegalArgumentException("reasonCode는 필수입니다.");

        if (request.getReasonCode() == ReasonCode.ETC) {
            if (request.getCustomText() == null || request.getCustomText().isBlank()) {
                throw new IllegalArgumentException("reasonCode가 ETC이면 customText는 필수입니다.");
            }
        }
    }

    private void validateOutcomeRequest(TodoDto.GiveUpAndRetryRequest request) {
        if (request == null) throw new IllegalArgumentException("사유 입력이 필요합니다.");
        if (request.getReasonCode() == null) throw new IllegalArgumentException("reasonCode는 필수입니다.");

        if (request.getReasonCode() == ReasonCode.ETC) {
            if (request.getCustomText() == null || request.getCustomText().isBlank()) {
                throw new IllegalArgumentException("reasonCode가 ETC이면 customText는 필수입니다.");
            }
        }
    }

    private LocalDate resolveDueDate(DueType dueType, LocalDate requestDueDate) {
        LocalDate today = LocalDate.now();

        return switch (dueType) {
            case TODAY -> today;
            case TOMORROW -> today.plusDays(1);
            case CUSTOM -> {
                if (requestDueDate == null) throw new IllegalArgumentException("CUSTOM dueType은 dueDate가 필수입니다.");
                yield requestDueDate;
            }
        };
    }

    private LocalDate resolveRetryDueDate(DueType dueType, LocalDate requestDueDate) {
        LocalDate today = LocalDate.now();

        if (dueType == DueType.TOMORROW) return today.plusDays(1);
        if (requestDueDate == null) throw new IllegalArgumentException("CUSTOM 재도전은 dueDate가 필수입니다.");
        return requestDueDate;
    }

    private Todo findTodo(Long todoId, Long userId) {
        return todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("투두가 존재하지 않습니다."));
    }

    private void validateVerifiable(Todo todo, VerificationType expectedType) {
        if (todo.getTodoStatus() != todoStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 투두만 인증할 수 있습니다.");
        }
        if (todo.getVerificationType() != expectedType) {
            throw new IllegalArgumentException("해당 투두의 인증 방식과 요청 API가 일치하지 않습니다.");
        }
        if (todoVerificationRepository.existsByTodoId(todo.getId())) {
            throw new IllegalStateException("이미 인증 처리된 투두입니다.");
        }
    }

    private TodoDto.VerificationResponse toVerificationResponse(Todo todo, LocalDateTime verifiedAt) {
        return TodoDto.VerificationResponse.builder()
                .todoId(todo.getId())
                .todoStatus(todo.getTodoStatus())
                .verifiedAt(verifiedAt)
                .build();
    }

    private TodoDto.SummaryResponse toSummary(Todo todo) {
        return TodoDto.SummaryResponse.builder()
                .todoId(todo.getId())
                .categoryId(todo.getCategory().getId())
                .categoryName(todo.getCategory().getName())
                .content(todo.getContent())
                .dueDate(todo.getDueDate())
                .dueType(todo.getDueType())
                .repeatType(todo.getRepeatType())
                .segmentationType(todo.getSegmentationType())
                .verificationType(todo.getVerificationType())
                .todoStatus(todo.getTodoStatus())
                .build();
    }

    private TodoDto.DetailResponse toDetail(Todo todo) {
        return TodoDto.DetailResponse.builder()
                .todoId(todo.getId())
                .categoryId(todo.getCategory().getId())
                .categoryName(todo.getCategory().getName())
                .content(todo.getContent())
                .dueDate(todo.getDueDate())
                .dueType(todo.getDueType())
                .repeatType(todo.getRepeatType())
                .segmentationType(todo.getSegmentationType())
                .verificationType(todo.getVerificationType())
                .todoStatus(todo.getTodoStatus())
                .build();
    }
}

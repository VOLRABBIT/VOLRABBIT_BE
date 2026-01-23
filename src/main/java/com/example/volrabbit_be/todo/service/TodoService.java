package com.example.volrabbit_be.todo.service;

import com.example.volrabbit_be.global.exception.ApiExceptions;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TodoOutcomeRepository todoOutcomeRepository;
    private final TodoVerificationRepository todoVerificationRepository;
    private final TodoSegmentationService todoSegmentationService;

    private static final int DAILY_DAYS_AHEAD = 30;     // 기준일부터 30일치 추가
    private static final int WEEKLY_WEEKS_AHEAD = 12;   // 12주치 추가
    private static final int MONTHLY_MONTHS_AHEAD = 12; // 12개월치 추가

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
                .orElseThrow(() -> new ApiExceptions.NotFound("사용자가 존재하지 않습니다."));

        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("카테고리가 존재하지 않습니다."));

        if (!category.isActive()) {
            throw new ApiExceptions.Conflict("비활성화된 카테고리는 선택할 수 없습니다.");
        }

        LocalDate dueDate = resolveDueDate(request.getDueType(), request.getDueDate());
        validateDueDateNotPast(dueDate);

        // 반복이면 여러 개 생성 + repeatGroupId 묶기
        String repeatGroupId = (request.getRepeatType() == RepeatType.NONE)
                ? null
                : UUID.randomUUID().toString();

        List<Todo> toSave = buildTodosForRepeat(user, category, request, dueDate, repeatGroupId);
        List<Todo> savedList = todoRepository.saveAll(toSave);

        // 가장 빠른 dueDate의 투두 1개를 대표로 응답/AI 호출
        Todo first = savedList.stream()
                .min((a, b) -> {
                    int c = a.getDueDate().compareTo(b.getDueDate());
                    if (c != 0) return c;
                    return Long.compare(
                            a.getId() == null ? Long.MAX_VALUE : a.getId(),
                            b.getId() == null ? Long.MAX_VALUE : b.getId()
                    );
                })
                .orElseThrow(() -> new ApiExceptions.BadRequest("투두 생성에 실패했습니다."));

        // 세분화 필요 시 호출
        todoSegmentationService.generateStepsIfNeeded(userId, first.getId(), request.getSegmentationType());

        return TodoDto.CreateResponse.builder()
                .todoId(first.getId())
                .repeatGroupId(repeatGroupId)
                .createdCount(savedList.size())
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
            throw new ApiExceptions.BadRequest("조회 날짜가 올바르지 않습니다.");
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
    public TodoDto.DetailResponse update(Long userId, Long todoId, TodoDto.UpdateRequest request) {
        if (request == null) {
            throw new ApiExceptions.BadRequest("요청 바디가 필요합니다.");
        }

        Todo todo = findTodo(todoId, userId);

        // 1) content (옵션)
        if (request.getContent() != null) {
            String content = request.getContent();
            if (content.isBlank()) throw new ApiExceptions.BadRequest("content는 비어 있을 수 없습니다.");
            if (content.length() > 10) throw new ApiExceptions.BadRequest("content는 최대 10자입니다.");
        }

        // 2) category (옵션)
        Category newCategory = null;
        if (request.getCategoryId() != null) {
            newCategory = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                    .orElseThrow(() -> new ApiExceptions.NotFound("카테고리가 존재하지 않습니다."));

            if (!newCategory.isActive()) {
                throw new ApiExceptions.Conflict("비활성화된 카테고리는 선택할 수 없습니다.");
            }
        }

        // 3) dueType / dueDate 규칙
        DueType newDueType = null;
        LocalDate newDueDate = null;

        if (request.getDueType() == null && request.getDueDate() != null) {
            throw new ApiExceptions.BadRequest("dueDate만 단독으로 수정할 수 없습니다. dueType과 함께 보내주세요.");
        }

        if (request.getDueType() != null) {
            newDueType = request.getDueType();

            if (newDueType == DueType.CUSTOM) {
                if (request.getDueDate() == null) {
                    throw new ApiExceptions.BadRequest("dueType이 CUSTOM이면 dueDate는 필수입니다.");
                }
                newDueDate = request.getDueDate();
            } else {
                newDueDate = resolveDueDate(newDueType, null);
            }

            validateDueDateNotPast(newDueDate);
        }

        // 4) repeatType 변경 시 repeatGroupId 처리
        String newRepeatGroupId = resolveRepeatGroupIdForUpdate(todo, request.getRepeatType());

        // 5) patch 적용
        todo.patchUpdate(
                newCategory,
                request.getContent(),
                newDueDate,
                newDueType,
                request.getRepeatType(),
                newRepeatGroupId,
                request.getSegmentationType(),
                request.getVerificationType()
        );

        return toDetail(todo);
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
        todo.giveUp();

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

    @Transactional
    public TodoDto.RetryResponse giveUpAndRetry(Long userId, Long todoId, TodoDto.GiveUpAndRetryRequest request) {
        validateOutcomeRequest(request);

        Todo original = findTodo(todoId, userId);

        original.giveUp();

        TodoOutcome outcome = TodoOutcome.builder()
                .todo(original)
                .outcomeStatus(OutcomeStatus.GIVE_UP)
                .reasonCode(request.getReasonCode())
                .customText(request.getCustomText())
                .build();
        todoOutcomeRepository.save(outcome);

        DueType newDueType = request.getDueType();
        if (newDueType != DueType.TOMORROW && newDueType != DueType.CUSTOM) {
            throw new ApiExceptions.BadRequest("재도전 dueType은 TOMORROW 또는 CUSTOM만 가능합니다.");
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

    @Transactional
    public TodoDto.RetryResponse retry(Long userId, Long todoId, TodoDto.RetryRequest request) {
        if (request == null) {
            throw new ApiExceptions.BadRequest("요청 바디가 필요합니다.");
        }

        Todo original = findTodo(todoId, userId);

        if (original.getTodoStatus() != todoStatus.FAIL && original.getTodoStatus() != todoStatus.GIVE_UP) {
            throw new ApiExceptions.Conflict("실패/포기한 투두만 재도전할 수 있습니다.");
        }

        DueType newDueType = request.getDueType();
        if (newDueType != DueType.TOMORROW && newDueType != DueType.CUSTOM) {
            throw new ApiExceptions.BadRequest("재도전 dueType은 TOMORROW 또는 CUSTOM만 가능합니다.");
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
     */
    @Transactional
    public int autoFailExpiredTodos(LocalDate today) {
        if (today == null) {
            throw new ApiExceptions.BadRequest("today 값이 필요합니다.");
        }

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
            throw new ApiExceptions.BadRequest("체크 인증 투두만 체크 해제가 가능합니다.");
        }

        if (todo.getTodoStatus() != todoStatus.SUCCESS) {
            throw new ApiExceptions.Conflict("완료 표시됐던 투두만 체크 해제할 수 있습니다.");
        }

        todoVerificationRepository.findByTodoId(todo.getId())
                .orElseThrow(() -> new ApiExceptions.NotFound("체크 인증 기록이 없습니다."));

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
            throw new ApiExceptions.BadRequest("photoUrl은 필수입니다.");
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
            throw new ApiExceptions.BadRequest("제대로 된 NFC 값이 입력되지 않았습니다.");
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
        if (categoryId == null) throw new ApiExceptions.BadRequest("categoryId는 필수입니다.");
        if (content == null || content.isBlank()) throw new ApiExceptions.BadRequest("content는 필수입니다.");
        if (content.length() > 10) throw new ApiExceptions.BadRequest("content는 최대 10자입니다.");
        if (dueType == null) throw new ApiExceptions.BadRequest("dueType은 필수입니다.");
        if (repeatType == null) throw new ApiExceptions.BadRequest("repeatType은 필수입니다.");
        if (segmentationType == null) throw new ApiExceptions.BadRequest("segmentationType은 필수입니다.");
        if (verificationType == null) throw new ApiExceptions.BadRequest("verificationType은 필수입니다.");
    }

    private void validateOutcomeRequest(TodoDto.OutcomeRequest request) {
        if (request == null) throw new ApiExceptions.BadRequest("사유 입력이 필요합니다.");
        if (request.getReasonCode() == null) throw new ApiExceptions.BadRequest("reasonCode는 필수입니다.");

        if (request.getReasonCode() == ReasonCode.ETC) {
            if (request.getCustomText() == null || request.getCustomText().isBlank()) {
                throw new ApiExceptions.BadRequest("reasonCode가 ETC이면 customText는 필수입니다.");
            }
        }
    }

    private void validateOutcomeRequest(TodoDto.GiveUpAndRetryRequest request) {
        if (request == null) throw new ApiExceptions.BadRequest("사유 입력이 필요합니다.");
        if (request.getReasonCode() == null) throw new ApiExceptions.BadRequest("reasonCode는 필수입니다.");

        if (request.getReasonCode() == ReasonCode.ETC) {
            if (request.getCustomText() == null || request.getCustomText().isBlank()) {
                throw new ApiExceptions.BadRequest("reasonCode가 ETC이면 customText는 필수입니다.");
            }
        }
    }

    private LocalDate resolveDueDate(DueType dueType, LocalDate requestDueDate) {
        LocalDate today = LocalDate.now();

        return switch (dueType) {
            case TODAY -> today;
            case TOMORROW -> today.plusDays(1);
            case CUSTOM -> {
                if (requestDueDate == null) throw new ApiExceptions.BadRequest("CUSTOM dueType은 dueDate가 필수입니다.");
                yield requestDueDate;
            }
        };
    }

    private LocalDate resolveRetryDueDate(DueType dueType, LocalDate requestDueDate) {
        LocalDate today = LocalDate.now();

        if (dueType == DueType.TOMORROW) return today.plusDays(1);
        if (requestDueDate == null) throw new ApiExceptions.BadRequest("CUSTOM 재도전은 dueDate가 필수입니다.");
        return requestDueDate;
    }

    private void validateDueDateNotPast(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            throw new ApiExceptions.BadRequest("dueDate는 과거 날짜일 수 없습니다.");
        }
    }

    /**
     * repeatType 변경 요청이 들어오면 repeatGroupId도 같이 정리
     * - null(변경없음) -> 기존 유지(null 반환해서 patchUpdate에서 건드리지 않게)
     * - NONE -> null로 끊기
     * - DAILY/WEEKLY/MONTHLY -> 기존 group이 있으면 유지, 없으면 새 UUID 발급
     */
    private String resolveRepeatGroupIdForUpdate(Todo todo, RepeatType newRepeatType) {
        if (newRepeatType == null) return null;

        if (newRepeatType == RepeatType.NONE) {
            return null;
        }

        // 반복으로 바꾸는 경우
        if (todo.getRepeatGroupId() != null) return todo.getRepeatGroupId();
        return UUID.randomUUID().toString();
    }

    /**
     * repeatType에 따라 "기준 dueDate부터 미래 일정"을 미리 생성해서 반환
     * - NONE: 1개
     * - DAILY: 기준일부터 30일치 추가
     * - WEEKLY: 기준일부터 12주치 추가
     * - MONTHLY: 기준일부터 12개월치 추가
     */
    private List<Todo> buildTodosForRepeat(
            User user,
            Category category,
            TodoDto.CreateRequest request,
            LocalDate baseDueDate,
            String repeatGroupId
    ) {
        List<LocalDate> dates = buildRepeatDates(baseDueDate, request.getRepeatType());

        List<Todo> list = new ArrayList<>(dates.size());
        for (LocalDate date : dates) {
            Todo todo = Todo.builder()
                    .user(user)
                    .category(category)
                    .content(request.getContent())
                    .dueType(request.getDueType())
                    .dueDate(date)
                    .repeatType(request.getRepeatType())
                    .repeatGroupId(repeatGroupId)
                    .segmentationType(request.getSegmentationType())
                    .verificationType(request.getVerificationType())
                    .build();
            list.add(todo);
        }
        return list;
    }

    private List<LocalDate> buildRepeatDates(LocalDate base, RepeatType repeatType) {
        List<LocalDate> dates = new ArrayList<>();
        dates.add(base); // 기준일은 항상 생성

        switch (repeatType) {
            case NONE -> {
            }
            case DAILY -> {
                for (int i = 1; i <= DAILY_DAYS_AHEAD; i++) dates.add(base.plusDays(i));
            }
            case WEEKLY -> {
                for (int i = 1; i <= WEEKLY_WEEKS_AHEAD; i++) dates.add(base.plusWeeks(i));
            }
            case MONTHLY -> {
                for (int i = 1; i <= MONTHLY_MONTHS_AHEAD; i++) dates.add(base.plusMonths(i));
            }
        }
        return dates;
    }

    private Todo findTodo(Long todoId, Long userId) {
        return todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new ApiExceptions.NotFound("투두가 존재하지 않습니다."));
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
                .repeatGroupId(todo.getRepeatGroupId())
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
                .repeatGroupId(todo.getRepeatGroupId())
                .segmentationType(todo.getSegmentationType())
                .verificationType(todo.getVerificationType())
                .todoStatus(todo.getTodoStatus())
                .build();
    }
}

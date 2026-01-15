package com.example.volrabbit_be.onboarding.service;

import com.example.volrabbit_be.onboarding.dto.OnboardingDto;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    @Value("${volrabbit.onboarding.question.filename:classpath:onboarding/questions.json}")
    private Resource questionJsonPath;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository; // 페르소나 지정할 때 필요

    /**
     * 모든 온보딩 질문을 가져옴
     * @return 온보딩 질문 응답 객체로 구성된 리스트
     */
    public List<OnboardingDto.OnboardingQuestionResponse> getAllQuestions() {
        try (FileReader fr = new FileReader(questionJsonPath.getFile())) {
            List<OnboardingDto.OnboardingQuestionResponse> onboardingQuestions =
                    objectMapper.readValue(fr, new TypeReference<List<OnboardingDto.OnboardingQuestionResponse>>() {});

            return onboardingQuestions;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("온보딩 질문 파일이 존재하지 않습니다.");
        } catch (DatabindException e) {
            throw new IllegalArgumentException("온보딩 질문 파일이 잘못되었습니다.");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 온보딩 질문에 대한 응답을 처리하고, 해당 유저에 페르소나 지정
     * @param userId 유저 ID
     * @param onboardingSubmits 온보딩 질문에 대한 응답
     * @return 온보딩 결과(유저 ID, 지정된 페르소나 ID) 객체
     */
    @Transactional
    public OnboardingDto.OnboardingResultResponse submitResponses(
            long userId, List<OnboardingDto.OnboardingSubmitRequest> onboardingSubmits) {
        long personaId = processResponses(onboardingSubmits);
        assignPersonaToUser(userId, personaId);

        return OnboardingDto.OnboardingResultResponse.of(userId, personaId);
    }

    private long processResponses(List<OnboardingDto.OnboardingSubmitRequest> onboardingSubmits) {
        // 페르소나명, 아이디 매핑
        Map<String, Long> personaIdMap = new HashMap<>();
        personaIdMap.put("Time Wizard", 1L);
        personaIdMap.put("Starter-Freeze Bot", 2L);
        personaIdMap.put("Attention-thief", 3L);
        personaIdMap.put("Emotional Adventure", 4L);

        // 응답이 아예 비어있을 때
        if (onboardingSubmits == null || onboardingSubmits.isEmpty()) {
            return personaIdMap.get("Starter-Freeze Bot");
        }

        // 불완전한 응답 (온보딩 질문 개수 7개)
        if (onboardingSubmits.size() != 7) {
            throw new IllegalArgumentException("온보딩 제출 정보가 잘못되었습니다.");
        }

        // 혹시 몰라서 정렬 (최악의 경우 O(n log n))
        onboardingSubmits.sort(Comparator.comparing(OnboardingDto.OnboardingSubmitRequest::getQuestionNo));

        if (questionResponseFromList(onboardingSubmits, 3) >= 2
                || questionResponseFromList(onboardingSubmits, 4) >= 2) {
            return personaIdMap.get("Time Wizard");
        }

        if (questionResponseFromList(onboardingSubmits, 1) >= 2
                || questionResponseFromList(onboardingSubmits, 2) >= 2) {
            return personaIdMap.get("Starter-Freeze Bot");
        }

        if (questionResponseFromList(onboardingSubmits, 5) >= 2) {
            return personaIdMap.get("Attention-thief");
        }

        if (questionResponseFromList(onboardingSubmits, 6) >= 2
                || questionResponseFromList(onboardingSubmits, 7) >= 2) {
            return personaIdMap.get("Emotional Adventure");
        }

        return personaIdMap.get("Starter-Freeze Bot"); // 기본값
    }

    // 온보딩 제출 정보 리스트에서 해당하는 질문 번호에 대한 응답을 가져옴
    private int questionResponseFromList(List<OnboardingDto.OnboardingSubmitRequest> onboardingSubmits, int questionNo) {
        return onboardingSubmits.get(questionNoToIndex(questionNo)).getQuestionResponse();
    }

    // 1부터 시작하는 질문 번호를 0부터 시작하는 리스트 인덱스로 변환
    private int questionNoToIndex(int questionIndex) {
        if (questionIndex <= 0) throw new IllegalArgumentException("질문 번호는 1 이상이어야 합니다.");
        return questionIndex - 1;
    }

    // 해당 유저의 페르소나 ID 변경(지정)
    private void assignPersonaToUser(long userId, long personaId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        User user = optionalUser.orElseThrow(() -> new IllegalArgumentException("유저 ID에 해당하는 유저가 존재하지 않습니다."));

        // 이미 해당 유저의 페르소나가 지정되어 있는지 확인(온보딩에서만)
        if (user.getPersonaId() != null) throw new IllegalArgumentException("이미 해당 유저의 페르소나가 지정되어 있습니다.");

        user.selectPersona(personaId);
        userRepository.save(user);
    }
}

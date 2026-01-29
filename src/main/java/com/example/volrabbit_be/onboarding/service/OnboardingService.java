package com.example.volrabbit_be.onboarding.service;

import com.example.volrabbit_be.global.exception.ApiExceptions;
import com.example.volrabbit_be.onboarding.dto.OnboardingDto;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    // 온보딩 질문 파일(JSON)이 저장되어 있는 S3 경로 (우선 사용)
    @Value("${volrabbit.onboarding.question.s3filename:#null}")
    private Resource questionJsonS3Path;

    // 온보딩 질문 파일(JSON)이 저장되어 있는 로컬 경로 (로컬 테스트, S3 오류 대비)
    @Value("${volrabbit.onboarding.question.localfilename:classpath:onboarding/questions.json}")
    private Resource questionJsonLocalPath;

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository; // 페르소나 지정할 때 필요

    /**
     * 모든 온보딩 질문을 가져옴
     * @return 온보딩 질문 응답 객체로 구성된 리스트
     */
    public List<OnboardingDto.OnboardingQuestionResponse> getAllQuestions() {
        // 만약, S3 장애, 설정 오류 등으로 S3 객체를 사용하지 못할 때, 로컬 파일을 사용할 수 있도록 함
        try (InputStream is = getAllQuestionsInputStream(true)) {
            List<OnboardingDto.OnboardingQuestionResponse> onboardingQuestions =
                    objectMapper.readValue(is, new TypeReference<List<OnboardingDto.OnboardingQuestionResponse>>() {
                    });

            return onboardingQuestions;
        } catch (NoSuchKeyException e) {
            // localFallback을 false로 호출했을 때를 대비
            throw new ApiExceptions.NotFound("온보딩 질문 파일이 존재하지 않습니다. (S3)");
        } catch (FileNotFoundException e) {
            throw new ApiExceptions.NotFound("온보딩 질문 파일이 존재하지 않습니다. (로컬)");
        } catch (DatabindException e) {
            // localFallback을 false로 호출했을 땐 S3 객체, true로 호출했을 땐 로컬 파일
            throw new RuntimeException("온보딩 질문 파일이 잘못되었습니다.");
        } catch (IOException e) {
            // localFallback을 false로 호출했을 땐 S3 객체, true로 호출했을 땐 로컬 파일
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

    private InputStream getAllQuestionsInputStream(boolean localFallback) throws IOException {
        try {
            return getAllQuestionsInputStreamFromResource(questionJsonS3Path);
        } catch (Exception e) {
            if (!localFallback) {
                throw e;
            }

            return getAllQuestionsInputStreamFromResource(questionJsonLocalPath);
        }
    }

    private InputStream getAllQuestionsInputStreamFromResource(Resource resource) throws IOException {
        return resource.getInputStream();
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
            throw new ApiExceptions.BadRequest("온보딩 제출 정보가 잘못되었습니다.");
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

        User user = optionalUser.orElseThrow(() -> new ApiExceptions.BadRequest("유저 ID에 해당하는 유저가 존재하지 않습니다."));

        // 이미 해당 유저의 페르소나가 지정되어 있는지 확인(온보딩에서만)
        if (user.getPersonaId() != null) throw new ApiExceptions.BadRequest("이미 해당 유저의 페르소나가 지정되어 있습니다.");

        user.selectPersona(personaId);
        userRepository.save(user);
    }
}

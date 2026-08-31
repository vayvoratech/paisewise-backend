package in.sapphirus.rupee.learn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.learn.domain.QuizQuestion;
import in.sapphirus.rupee.learn.domain.QuizAttempt;
import in.sapphirus.rupee.learn.repo.QuizRepository;
import in.sapphirus.rupee.learn.repo.QuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing quiz loading, shuffling, anti-cheating serialization, submissions scoring, and attempt logs.
 */
@Service
public class QuizService {

    private final QuizRepository quizRepo;
    private final QuizAttemptRepository attemptRepo;
    private final XpService xpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(QuizRepository quizRepo, QuizAttemptRepository attemptRepo, XpService xpService) {
        this.quizRepo = quizRepo;
        this.attemptRepo = attemptRepo;
        this.xpService = xpService;
    }

    public List<QuizQuestion> getQuizForLesson(String lessonId) {
        List<QuizQuestion> list = quizRepo.findByLessonId(lessonId);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quiz found for lesson: " + lessonId);
        }

        // Shuffle questions
        Collections.shuffle(list);

        // Parse, shuffle choices, and strip 'correct' flag to prevent inspection cheating
        return list.stream().map(q -> {
            String safeOptionsJson = shuffleAndStripOptions(q.getOptionsJson());
            QuizQuestion safe = new QuizQuestion(
                    q.getId(),
                    q.getLessonId(),
                    "HIDDEN_FOR_SECURITY", // Strip direct correctOptionId in responses
                    q.getPrompt(),
                    q.getSeconds(),
                    q.getXp(),
                    q.getOrderNo(),
                    safeOptionsJson,
                    q.getExplanation()
            );
            return safe;
        }).collect(Collectors.toList());
    }

    @Transactional
    public QuizAttempt submitQuiz(UUID userId, String lessonId, List<String> userAnswers, int xpReward) {
        List<QuizQuestion> questions = quizRepo.findByLessonId(lessonId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found for lesson: " + lessonId);
        }
        if (questions.size() != userAnswers.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submitted answers count does not match quiz size");
        }

        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            String correctKey = q.getCorrectOptionId();
            if (correctKey == null || "HIDDEN_FOR_SECURITY".equals(correctKey)) {
                // Fallback: parse from optionsJson if correctOptionId field is unpopulated
                correctKey = getCorrectOptionKeyFromOptionsJson(q.getOptionsJson());
            }
            if (correctKey != null && correctKey.equalsIgnoreCase(userAnswers.get(i))) {
                score++;
            }
        }

        double scorePct = ((double) score / questions.size()) * 100.0;

        // Log attempt number dynamically
        int attemptNum = attemptRepo.findByUserIdAndLessonIdOrderByAttemptNumberDesc(userId, lessonId)
                .stream().findFirst().map(QuizAttempt::getAttemptNumber).orElse(0) + 1;

        QuizAttempt attempt = new QuizAttempt(userId, lessonId, attemptNum);
        attempt.setTotalQuestions(questions.size());
        attempt.setCorrectAnswers(score);
        attempt.setScorePct(scorePct);
        
        boolean passed = scorePct >= attempt.getPassThresholdPct();
        attempt.setPassed(passed);
        attempt.setStatus("COMPLETED");
        attempt.setSubmittedAt(Instant.now());
        attempt.setCompletedAt(Instant.now());

        // Check if there is an existing passed attempt
        boolean alreadyPassed = attemptRepo.findByUserIdAndLessonIdOrderByAttemptNumberDesc(userId, lessonId)
                .stream().anyMatch(QuizAttempt::isPassed);

        if (passed && !alreadyPassed) {
            // Award XP only on the first passed attempt to prevent spam farming
            attempt.setXpEarned(xpReward);
            xpService.awardXp(userId, xpReward, "FIRST_QUIZ_PASS_" + lessonId);
        } else {
            attempt.setXpEarned(0);
        }

        try {
            attempt.setUserAnswers(objectMapper.writeValueAsString(userAnswers));
            List<String> questionIds = questions.stream().map(QuizQuestion::getId).collect(Collectors.toList());
            attempt.setQuestionsServed(objectMapper.writeValueAsString(questionIds));
        } catch (Exception e) {
            // fallback serialization safeguard
        }

        return attemptRepo.save(attempt);
    }

    public List<QuizAttempt> getQuizHistory(UUID userId) {
        return attemptRepo.findByUserIdOrderByStartedAtDesc(userId);
    }

    // --- Helpers ---

    private String shuffleAndStripOptions(String optionsJson) {
        try {
            List<Map<String, Object>> optionsList = objectMapper.readValue(optionsJson, new TypeReference<List<Map<String, Object>>>() {});
            Collections.shuffle(optionsList);
            for (Map<String, Object> opt : optionsList) {
                opt.remove("correct");
            }
            return objectMapper.writeValueAsString(optionsList);
        } catch (Exception e) {
            return optionsJson;
        }
    }

    private String getCorrectOptionKeyFromOptionsJson(String optionsJson) {
        try {
            List<Map<String, Object>> optionsList = objectMapper.readValue(optionsJson, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> opt : optionsList) {
                if (Boolean.TRUE.equals(opt.get("correct"))) {
                    return (String) opt.get("key");
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}

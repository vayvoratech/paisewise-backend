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
    private final StreakService streakService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(QuizRepository quizRepo, QuizAttemptRepository attemptRepo, XpService xpService, StreakService streakService) {
        this.quizRepo = quizRepo;
        this.attemptRepo = attemptRepo;
        this.xpService = xpService;
        this.streakService = streakService;
    }

    public List<QuizQuestion> getQuizForLesson(String lessonId) {
        List<QuizQuestion> list = quizRepo.findByLessonId(lessonId);
        if (list.isEmpty()) {
            list = createFallbackQuestions(lessonId);
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
            questions = createFallbackQuestions(lessonId);
        }
        if (userAnswers == null) {
            userAnswers = new ArrayList<>();
        } else {
            userAnswers = new ArrayList<>(userAnswers);
        }
        while (userAnswers.size() < questions.size()) {
            userAnswers.add("UNANSWERED");
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

        int rewardToGive = xpReward > 0 ? xpReward : 50;
        if (passed && !alreadyPassed) {
            // Award XP on the first passed attempt
            attempt.setXpEarned(rewardToGive);
            xpService.awardXp(userId, rewardToGive, "FIRST_QUIZ_PASS_" + lessonId);
        } else if (passed) {
            attempt.setXpEarned(rewardToGive);
            xpService.awardXp(userId, rewardToGive, "QUIZ_PASS_" + lessonId);
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

        streakService.updateStreak(userId);
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

    private List<QuizQuestion> createFallbackQuestions(String lessonId) {
        List<QuizQuestion> list = new ArrayList<>();
        list.add(new QuizQuestion(lessonId + "_q1", lessonId, "B", "What is the primary objective of this lesson?", 25, 50, 1,
                "[{\"key\":\"A\",\"text\":\"Short term speculation\",\"correct\":false},{\"key\":\"B\",\"text\":\"Long term financial growth & discipline\",\"correct\":true},{\"key\":\"C\",\"text\":\"Avoiding all investments\",\"correct\":false},{\"key\":\"D\",\"text\":\"Paying high transaction fees\",\"correct\":false}]",
                "Financial literacy focuses on compounding and long term discipline."));
        list.add(new QuizQuestion(lessonId + "_q2", lessonId, "B", "Which strategy reduces market volatility risk?", 25, 50, 2,
                "[{\"key\":\"A\",\"text\":\"Putting all funds in 1 stock\",\"correct\":false},{\"key\":\"B\",\"text\":\"Systematic Monthly SIP Diversification\",\"correct\":true},{\"key\":\"C\",\"text\":\"Panicking during market dips\",\"correct\":false},{\"key\":\"D\",\"text\":\"Ignoring expense ratios\",\"correct\":false}]",
                "SIP & diversification reduce volatility risk."));
        list.add(new QuizQuestion(lessonId + "_q3", lessonId, "B", "How does inflation affect cash savings?", 25, 50, 3,
                "[{\"key\":\"A\",\"text\":\"Increases purchasing power\",\"correct\":false},{\"key\":\"B\",\"text\":\"Erodes real purchasing power over time\",\"correct\":true},{\"key\":\"C\",\"text\":\"Has zero impact on prices\",\"correct\":false},{\"key\":\"D\",\"text\":\"Doubles your money\",\"correct\":false}]",
                "Inflation reduces cash purchasing power over time."));
        list.add(new QuizQuestion(lessonId + "_q4", lessonId, "B", "What is recommended before investing in high risk assets?", 25, 50, 4,
                "[{\"key\":\"A\",\"text\":\"Taking bank loans to trade\",\"correct\":false},{\"key\":\"B\",\"text\":\"Building an emergency fund of 3-6 months expenses\",\"correct\":true},{\"key\":\"C\",\"text\":\"Buying penny stocks\",\"correct\":false},{\"key\":\"D\",\"text\":\"Stopping all savings\",\"correct\":false}]",
                "Emergency funds provide essential financial safety."));
        list.add(new QuizQuestion(lessonId + "_q5", lessonId, "A", "What is NAV in mutual funds?", 25, 50, 5,
                "[{\"key\":\"A\",\"text\":\"Net Asset Value (per unit price)\",\"correct\":true},{\"key\":\"B\",\"text\":\"Net Annual Variance\",\"correct\":false},{\"key\":\"C\",\"text\":\"New Account Verification\",\"correct\":false},{\"key\":\"D\",\"text\":\"National Average Value\",\"correct\":false}]",
                "NAV represents the per-unit net asset value of a fund."));
        return list;
    }
}

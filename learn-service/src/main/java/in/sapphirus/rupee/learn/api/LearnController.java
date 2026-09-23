package in.sapphirus.rupee.learn.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import in.sapphirus.rupee.learn.domain.JargonTerm;
import in.sapphirus.rupee.learn.domain.Lesson;
import in.sapphirus.rupee.learn.domain.QuizQuestion;
import in.sapphirus.rupee.learn.repo.QuizRepository;
import in.sapphirus.rupee.learn.service.LearnService;
import in.sapphirus.rupee.learn.service.QuizService;
import in.sapphirus.rupee.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing lessons, jargon glossary, and progress tracking APIs.
 * Supports both /learn and /learning gateway route mappings.
 */
@RestController
@RequestMapping({"/learn", "/learning"})
public class LearnController {

    private final LearnService learnService;
    private final QuizRepository quiz;
    private final QuizService quizService;
    private final in.sapphirus.rupee.learn.service.StreakService streakService;

    public LearnController(LearnService learnService, QuizRepository quiz, QuizService quizService, in.sapphirus.rupee.learn.service.StreakService streakService) {
        this.learnService = learnService;
        this.quiz = quiz;
        this.quizService = quizService;
        this.streakService = streakService;
    }

    public record LessonView(String id, String chapter, int chapterNo, int index, int total,
                             String title, int quizXp,
                             @JsonRawValue String segments, @JsonRawValue String jargonWords) {}

    public record JargonView(String term, String definition, String analogy, String example) {}

    public record QuizView(String id, String prompt, int seconds, int xp,
                           @JsonRawValue String options, String explanation) {}

    public record ProgressRequest(String lessonId) {}

    public record ProgressResponse(double progressPercent, String completedLessonId) {
        public ProgressResponse(double progressPercent) {
            this(progressPercent, null);
        }
    }

    public record UserProgressResponse(double progressPercent, List<String> completedLessonIds) {}

    public record QuizSubmitRequest(List<String> answers, int xpReward) {}

    @GetMapping("/lessons")
    public List<LessonView> allLessons() {
        return learnService.getLessons().stream().map(this::lessonView).toList();
    }

    @GetMapping("/lessons/{id}")
    public LessonView lesson(@PathVariable String id) {
        return lessonView(learnService.getLesson(id));
    }

    public record AiJargonRequest(String term, String language) {}
    public record AiJargonResponse(String term, String language, String explanation) {}

    @GetMapping("/jargon/{term}")
    public JargonView jargon(@PathVariable String term, @RequestParam(required = false, defaultValue = "en") String language) {
        try {
            JargonTerm t = learnService.getJargonTerm(term);
            return new JargonView(t.getTerm(), t.getDefinition(), t.getAnalogy(), t.getExample());
        } catch (Exception e) {
            String explanation = learnService.getAiJargonExplanation(term, language);
            return new JargonView(term, explanation, "", "");
        }
    }

    @RequestMapping(value = {"/jargon/ai", "/ai/jargon", "/api/jargon/ai"}, method = {RequestMethod.GET, RequestMethod.POST})
    public AiJargonResponse aiJargon(
            @RequestParam(required = false) String term,
            @RequestParam(required = false, defaultValue = "en") String language,
            @RequestBody(required = false) AiJargonRequest req) {
        String targetTerm = (term != null && !term.isBlank()) ? term : (req != null ? req.term() : "SIP");
        String targetLang = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language)) ? language : (req != null && req.language() != null ? req.language() : language);
        String explanation = learnService.getAiJargonExplanation(targetTerm, targetLang);
        return new AiJargonResponse(targetTerm, targetLang, explanation);
    }

    @GetMapping("/quiz/daily")
    public List<QuizView> dailyQuiz() {
        return quiz.findAllByOrderByOrderNoAsc().stream()
                .map(q -> new QuizView(q.getId(), q.getPrompt(), q.getSeconds(), q.getXp(),
                        q.getOptionsJson(), q.getExplanation()))
                .toList();
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public ProgressResponse completeLessonByPath(@PathVariable String lessonId) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        learnService.completeLesson(userId, lessonId);
        double percent = learnService.getLessonProgress(userId);
        return new ProgressResponse(percent, lessonId);
    }

    @PostMapping({"/progress", "/complete"})
    public ProgressResponse completeLessonByBody(@RequestBody ProgressRequest req) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        learnService.completeLesson(userId, req.lessonId());
        double percent = learnService.getLessonProgress(userId);
        return new ProgressResponse(percent, req.lessonId());
    }

    @PostMapping("/viewed")
    public void markLessonViewed(@RequestBody ProgressRequest req) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        learnService.markLessonViewed(userId, req.lessonId());
    }

    @GetMapping("/progress")
    public ProgressResponse getProgress() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        double percent = learnService.getLessonProgress(userId);
        return new ProgressResponse(percent);
    }

    @GetMapping("/user-progress")
    public UserProgressResponse getUserProgress() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        double percent = learnService.getLessonProgress(userId);
        List<String> completed = learnService.getCompletedLessonIds(userId);
        return new UserProgressResponse(percent, completed);
    }

    @GetMapping({"/lessons/{lessonId}/quiz", "/quizzes/{lessonId}"})
    public List<QuizView> getLessonQuiz(@PathVariable String lessonId) {
        return quizService.getQuizForLesson(lessonId).stream()
                .map(q -> new QuizView(q.getId(), q.getPrompt(), q.getSeconds(), q.getXp(),
                        q.getOptionsJson(), q.getExplanation()))
                .toList();
    }

    @PostMapping({"/lessons/{lessonId}/quiz/submit", "/quizzes/{lessonId}/submit"})
    public in.sapphirus.rupee.learn.domain.QuizAttempt submitLessonQuiz(
            @PathVariable String lessonId,
            @RequestBody QuizSubmitRequest req) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return quizService.submitQuiz(userId, lessonId, req.answers(), req.xpReward());
    }

    @GetMapping("/quiz/history")
    public List<in.sapphirus.rupee.learn.domain.QuizAttempt> getQuizHistory() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return quizService.getQuizHistory(userId);
    }

    @GetMapping("/streak")
    public in.sapphirus.rupee.learn.service.StreakService.StreakView getStreak() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return streakService.getStreak(userId);
    }

    private LessonView lessonView(Lesson l) {
        return new LessonView(l.getId(), l.getChapter(), l.getChapterNo(), l.getIndex(), l.getTotal(),
                l.getTitle(), l.getQuizXp(), l.getSegmentsJson(), l.getJargonWordsJson());
    }
}


package in.sapphirus.rupee.learn.service;

import in.sapphirus.rupee.learn.domain.QuizQuestion;
import in.sapphirus.rupee.learn.domain.QuizAttempt;
import in.sapphirus.rupee.learn.repo.QuizRepository;
import in.sapphirus.rupee.learn.repo.QuizAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    private final QuizRepository quizRepo = mock(QuizRepository.class);
    private final QuizAttemptRepository attemptRepo = mock(QuizAttemptRepository.class);
    private final XpService xpService = mock(XpService.class);
    private final StreakService streakService = mock(StreakService.class);

    private QuizService service;

    @BeforeEach
    void setUp() {
        reset(quizRepo, attemptRepo, xpService, streakService);
        service = new QuizService(quizRepo, attemptRepo, xpService, streakService);
    }

    @Test
    void getQuizForLesson_whenNotFound_throwsResponseStatusException() {
        when(quizRepo.findByLessonId("mf-1")).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> service.getQuizForLesson("mf-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No quiz found");
    }

    @Test
    void getQuizForLesson_shufflesAndStripsCorrectFlag() {
        String optionsJson = "[{\"key\":\"A\",\"text\":\"Opt 1\",\"correct\":true},{\"key\":\"B\",\"text\":\"Opt 2\",\"correct\":false}]";
        QuizQuestion q = new QuizQuestion("q1", "mf-3", "A", "Prompt", 15, 50, 1, optionsJson, "Explanation");

        when(quizRepo.findByLessonId("mf-3")).thenReturn(List.of(q));

        List<QuizQuestion> result = service.getQuizForLesson("mf-3");

        assertThat(result).hasSize(1);
        QuizQuestion safeQ = result.get(0);
        assertThat(safeQ.getCorrectOptionId()).isEqualTo("HIDDEN_FOR_SECURITY");
        assertThat(safeQ.getOptionsJson()).doesNotContain("\"correct\":true");
        assertThat(safeQ.getOptionsJson()).doesNotContain("\"correct\":false");
        assertThat(safeQ.getOptionsJson()).contains("Opt 1");
    }

    @Test
    void submitQuiz_savesAttemptWithCorrectScore() {
        UUID userId = UUID.randomUUID();
        String optionsJson = "[{\"key\":\"A\",\"text\":\"Opt 1\",\"correct\":true},{\"key\":\"B\",\"text\":\"Opt 2\",\"correct\":false}]";
        QuizQuestion q = new QuizQuestion("q1", "mf-3", "A", "Prompt", 15, 50, 1, optionsJson, "Explanation");

        when(quizRepo.findByLessonId("mf-3")).thenReturn(List.of(q));
        when(attemptRepo.findByUserIdAndLessonIdOrderByAttemptNumberDesc(userId, "mf-3")).thenReturn(Collections.emptyList());
        when(attemptRepo.save(any(QuizAttempt.class))).thenAnswer(i -> i.getArguments()[0]);

        QuizAttempt attempt = service.submitQuiz(userId, "mf-3", List.of("A"), 50);

        assertThat(attempt.getCorrectAnswers()).isEqualTo(1);
        assertThat(attempt.getScorePct()).isEqualTo(100.0);
        assertThat(attempt.isPassed()).isTrue();
        assertThat(attempt.getAttemptNumber()).isEqualTo(1);
        verify(xpService, times(1)).awardXp(userId, 50, "FIRST_QUIZ_PASS_mf-3");
    }

    @Test
    void submitQuiz_onSecondAttempt_doesNotAwardXp() {
        UUID userId = UUID.randomUUID();
        String optionsJson = "[{\"key\":\"A\",\"text\":\"Opt 1\",\"correct\":true},{\"key\":\"B\",\"text\":\"Opt 2\",\"correct\":false}]";
        QuizQuestion q = new QuizQuestion("q1", "mf-3", "A", "Prompt", 15, 50, 1, optionsJson, "Explanation");

        QuizAttempt attempt1 = new QuizAttempt(userId, "mf-3", 1);
        attempt1.setPassed(true);

        when(quizRepo.findByLessonId("mf-3")).thenReturn(List.of(q));
        when(attemptRepo.findByUserIdAndLessonIdOrderByAttemptNumberDesc(userId, "mf-3")).thenReturn(List.of(attempt1));
        when(attemptRepo.save(any(QuizAttempt.class))).thenAnswer(i -> i.getArguments()[0]);

        QuizAttempt attempt2 = service.submitQuiz(userId, "mf-3", List.of("A"), 50);

        assertThat(attempt2.getAttemptNumber()).isEqualTo(2);
        assertThat(attempt2.isPassed()).isTrue();
        verify(xpService, never()).awardXp(any(), anyInt(), anyString());
    }

    @Test
    void submitQuiz_whenScoreBelow70Percent_doesNotMarkPassed() {
        UUID userId = UUID.randomUUID();
        String opts = "[{\"key\":\"A\",\"text\":\"Opt 1\",\"correct\":true},{\"key\":\"B\",\"text\":\"Opt 2\",\"correct\":false}]";
        QuizQuestion q1 = new QuizQuestion("q1", "mf-3", "A", "P1", 15, 25, 1, opts, "E1");
        QuizQuestion q2 = new QuizQuestion("q2", "mf-3", "A", "P2", 15, 25, 2, opts, "E2");

        when(quizRepo.findByLessonId("mf-3")).thenReturn(List.of(q1, q2));
        when(attemptRepo.findByUserIdAndLessonIdOrderByAttemptNumberDesc(userId, "mf-3")).thenReturn(Collections.emptyList());
        when(attemptRepo.save(any(QuizAttempt.class))).thenAnswer(i -> i.getArguments()[0]);

        QuizAttempt attempt = service.submitQuiz(userId, "mf-3", List.of("B", "A"), 50);

        assertThat(attempt.getScorePct()).isEqualTo(50.0);
        assertThat(attempt.isPassed()).isFalse();
        verify(xpService, never()).awardXp(any(), anyInt(), anyString());
    }

    @Test
    void submitQuiz_whenAllAnswersWrong_calculatesZeroScore() {
        UUID userId = UUID.randomUUID();
        String opts = "[{\"key\":\"A\",\"text\":\"Opt 1\",\"correct\":true},{\"key\":\"B\",\"text\":\"Opt 2\",\"correct\":false}]";
        QuizQuestion q1 = new QuizQuestion("q1", "mf-3", "A", "P1", 15, 50, 1, opts, "E1");

        when(quizRepo.findByLessonId("mf-3")).thenReturn(List.of(q1));
        when(attemptRepo.findByUserIdAndLessonIdOrderByAttemptNumberDesc(userId, "mf-3")).thenReturn(Collections.emptyList());
        when(attemptRepo.save(any(QuizAttempt.class))).thenAnswer(i -> i.getArguments()[0]);

        QuizAttempt attempt = service.submitQuiz(userId, "mf-3", List.of("B"), 50);

        assertThat(attempt.getCorrectAnswers()).isEqualTo(0);
        assertThat(attempt.getScorePct()).isEqualTo(0.0);
        assertThat(attempt.isPassed()).isFalse();
    }

    @Test
    void submitQuiz_updatesStreakOnAttempt() {
        UUID userId = UUID.randomUUID();
        String opts = "[{\"key\":\"A\",\"text\":\"Opt 1\",\"correct\":true}]";
        QuizQuestion q1 = new QuizQuestion("q1", "mf-3", "A", "P1", 15, 50, 1, opts, "E1");

        when(quizRepo.findByLessonId("mf-3")).thenReturn(List.of(q1));
        when(attemptRepo.findByUserIdAndLessonIdOrderByAttemptNumberDesc(userId, "mf-3")).thenReturn(Collections.emptyList());
        when(attemptRepo.save(any(QuizAttempt.class))).thenAnswer(i -> i.getArguments()[0]);

        service.submitQuiz(userId, "mf-3", List.of("A"), 50);

        verify(streakService, times(1)).updateStreak(userId);
    }

    @Test
    void getQuizHistory_returnsUserAttemptsInDescOrder() {
        UUID userId = UUID.randomUUID();
        QuizAttempt a1 = new QuizAttempt(userId, "mf-1", 1);
        when(attemptRepo.findByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of(a1));

        List<QuizAttempt> history = service.getQuizHistory(userId);
        assertThat(history).hasSize(1);
        verify(attemptRepo, times(1)).findByUserIdOrderByStartedAtDesc(userId);
    }

    @Test
    void getQuizHistory_whenNoAttempts_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(attemptRepo.findByUserIdOrderByStartedAtDesc(userId)).thenReturn(Collections.emptyList());

        List<QuizAttempt> history = service.getQuizHistory(userId);
        assertThat(history).isEmpty();
    }

    @Test
    void submitQuiz_whenAnswerSizeMismatch_throwsResponseStatusException() {
        UUID userId = UUID.randomUUID();
        String opts = "[{\"key\":\"A\",\"text\":\"Opt 1\",\"correct\":true}]";
        QuizQuestion q1 = new QuizQuestion("q1", "mf-3", "A", "P1", 15, 50, 1, opts, "E1");

        when(quizRepo.findByLessonId("mf-3")).thenReturn(List.of(q1));

        assertThatThrownBy(() -> service.submitQuiz(userId, "mf-3", Collections.emptyList(), 50))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Submitted answers count does not match quiz size");
    }
}

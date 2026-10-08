package in.sapphirus.rupee.learn.service;

import in.sapphirus.rupee.learn.domain.Lesson;
import in.sapphirus.rupee.learn.domain.UserLessonProgress;
import in.sapphirus.rupee.learn.domain.JargonTerm;
import in.sapphirus.rupee.learn.repo.LessonRepository;
import in.sapphirus.rupee.learn.repo.UserLessonProgressRepository;
import in.sapphirus.rupee.learn.repo.JargonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LearnServiceTest {

    private final LessonRepository lessonRepo = mock(LessonRepository.class);
    private final UserLessonProgressRepository progressRepo = mock(UserLessonProgressRepository.class);
    private final JargonRepository jargonRepo = mock(JargonRepository.class);
    private final StreakService streakService = mock(StreakService.class);
    private final XpService xpService = mock(XpService.class);

    private LearnService service;

    @BeforeEach
    void setUp() {
        reset(lessonRepo, progressRepo, jargonRepo, streakService, xpService);
        service = new LearnService(lessonRepo, progressRepo, jargonRepo, streakService, xpService);
    }

    @Test
    void getLessons_returnsAllLessons() {
        Lesson l = new Lesson("mf-1", "Intro", 1, 1, 6, "Title", 50, "[]", "[]");
        when(lessonRepo.findAll()).thenReturn(List.of(l));
        List<Lesson> result = service.getLessons();
        assertThat(result).hasSize(1);
        verify(lessonRepo, times(1)).findAll();
    }

    @Test
    void getLesson_whenFound_returnsLesson() {
        Lesson l = new Lesson("mf-1", "Intro", 1, 1, 6, "Title", 50, "[]", "[]");
        when(lessonRepo.findById("mf-1")).thenReturn(Optional.of(l));
        Lesson result = service.getLesson("mf-1");
        assertThat(result.getId()).isEqualTo("mf-1");
    }

    @Test
    void getLesson_whenNotFound_throwsResponseStatusException() {
        when(lessonRepo.findById("mf-99")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLesson("mf-99"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Lesson not found");
    }

    @Test
    void markLessonViewed_whenNotStarted_setsStatusToInProgress() {
        UUID userId = UUID.randomUUID();
        String lessonId = "mf-1";
        UserLessonProgress progress = new UserLessonProgress(userId, lessonId);
        progress.setStatus("NOT_STARTED");

        when(progressRepo.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));
        when(progressRepo.save(any(UserLessonProgress.class))).thenAnswer(i -> i.getArguments()[0]);

        service.markLessonViewed(userId, lessonId);

        assertThat(progress.getStatus()).isEqualTo("IN_PROGRESS");
        verify(progressRepo, times(1)).save(progress);
        verify(streakService, times(1)).updateStreak(userId);
    }

    @Test
    void markLessonViewed_whenAlreadyCompleted_keepsCompletedStatus() {
        UUID userId = UUID.randomUUID();
        String lessonId = "mf-1";
        UserLessonProgress progress = new UserLessonProgress(userId, lessonId);
        progress.setStatus("COMPLETED");

        when(progressRepo.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));
        when(progressRepo.save(any(UserLessonProgress.class))).thenAnswer(i -> i.getArguments()[0]);

        service.markLessonViewed(userId, lessonId);

        assertThat(progress.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void completeLesson_firstTime_awardsXpAndUpdatesStreak() {
        UUID userId = UUID.randomUUID();
        String lessonId = "mf-1";
        UserLessonProgress progress = new UserLessonProgress(userId, lessonId);
        progress.setStatus("IN_PROGRESS");

        when(progressRepo.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));
        when(progressRepo.save(any(UserLessonProgress.class))).thenAnswer(i -> i.getArguments()[0]);

        service.completeLesson(userId, lessonId);

        assertThat(progress.getStatus()).isEqualTo("COMPLETED");
        assertThat(progress.getCompletedAt()).isNotNull();
        verify(xpService, times(1)).awardXp(userId, 50, "LESSON_COMPLETE_mf-1");
        verify(streakService, times(1)).updateStreak(userId);
    }

    @Test
    void completeLesson_alreadyCompleted_doesNotAwardDuplicateXp() {
        UUID userId = UUID.randomUUID();
        String lessonId = "mf-1";
        UserLessonProgress progress = new UserLessonProgress(userId, lessonId);
        progress.setStatus("COMPLETED");

        when(progressRepo.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));
        when(progressRepo.save(any(UserLessonProgress.class))).thenAnswer(i -> i.getArguments()[0]);

        service.completeLesson(userId, lessonId);

        verify(xpService, never()).awardXp(any(), anyInt(), anyString());
    }

    @Test
    void getLessonProgress_whenNoLessons_returnsZero() {
        UUID userId = UUID.randomUUID();
        when(lessonRepo.count()).thenReturn(0L);
        double pct = service.getLessonProgress(userId);
        assertThat(pct).isEqualTo(0.0);
    }

    @Test
    void getLessonProgress_whenLessonsExist_returnsCorrectPercentage() {
        UUID userId = UUID.randomUUID();
        UserLessonProgress progress1 = new UserLessonProgress(userId, "mf-1");
        progress1.setStatus("COMPLETED");
        UserLessonProgress progress2 = new UserLessonProgress(userId, "mf-2");
        progress2.setStatus("IN_PROGRESS");

        when(lessonRepo.count()).thenReturn(4L);
        when(progressRepo.findByUserId(userId)).thenReturn(List.of(progress1, progress2));

        double pct = service.getLessonProgress(userId);
        assertThat(pct).isEqualTo(25.0);
    }

    @Test
    void getCompletedLessonIds_returnsOnlyCompletedLessonIds() {
        UUID userId = UUID.randomUUID();
        UserLessonProgress p1 = new UserLessonProgress(userId, "mf-1");
        p1.setStatus("COMPLETED");
        UserLessonProgress p2 = new UserLessonProgress(userId, "mf-2");
        p2.setStatus("IN_PROGRESS");

        when(progressRepo.findByUserId(userId)).thenReturn(List.of(p1, p2));

        List<String> completed = service.getCompletedLessonIds(userId);
        assertThat(completed).containsExactly("mf-1");
    }

    @Test
    void getJargonTerm_caseInsensitiveAndDashToSpace_returnsTerm() {
        JargonTerm t = new JargonTerm("Mutual Fund", "Def", "Analogy", "Ex");
        when(jargonRepo.findByTermIgnoreCase("mutual-fund")).thenReturn(Optional.empty());
        when(jargonRepo.findByTermIgnoreCase("mutual fund")).thenReturn(Optional.of(t));

        JargonTerm result = service.getJargonTerm("mutual-fund");
        assertThat(result.getTerm()).isEqualTo("Mutual Fund");
    }

    @Test
    void getJargonTerm_whenNotFound_throwsResponseStatusException() {
        when(jargonRepo.findByTermIgnoreCase("Unknown")).thenReturn(Optional.empty());
        when(jargonRepo.findFirstByTermContainingIgnoreCase("Unknown")).thenReturn(Optional.empty());
        when(jargonRepo.findById("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJargonTerm("Unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Jargon term not found");
    }
}

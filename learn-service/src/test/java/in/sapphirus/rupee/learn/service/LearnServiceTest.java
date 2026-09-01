package in.sapphirus.rupee.learn.service;

import in.sapphirus.rupee.learn.domain.Lesson;
import in.sapphirus.rupee.learn.domain.UserLessonProgress;
import in.sapphirus.rupee.learn.domain.JargonTerm;
import in.sapphirus.rupee.learn.repo.LessonRepository;
import in.sapphirus.rupee.learn.repo.UserLessonProgressRepository;
import in.sapphirus.rupee.learn.repo.JargonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

    private LearnService service;

    @BeforeEach
    void setUp() {
        reset(lessonRepo, progressRepo, jargonRepo, streakService);
        service = new LearnService(lessonRepo, progressRepo, jargonRepo, streakService);
    }

    @Test
    void getLessons_returnsAllLessons() {
        when(lessonRepo.findAll()).thenReturn(Collections.emptyList());
        List<Lesson> result = service.getLessons();
        assertThat(result).isEmpty();
        verify(lessonRepo, times(1)).findAll();
    }

    @Test
    void getLesson_whenNotFound_throwsResponseStatusException() {
        when(lessonRepo.findById("mf-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLesson("mf-1"))
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
    }

    @Test
    void completeLesson_setsStatusToCompleted() {
        UUID userId = UUID.randomUUID();
        String lessonId = "mf-1";
        UserLessonProgress progress = new UserLessonProgress(userId, lessonId);
        progress.setStatus("IN_PROGRESS");

        when(progressRepo.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));
        when(progressRepo.save(any(UserLessonProgress.class))).thenAnswer(i -> i.getArguments()[0]);

        service.completeLesson(userId, lessonId);

        assertThat(progress.getStatus()).isEqualTo("COMPLETED");
        assertThat(progress.getCompletedAt()).isNotNull();
        verify(progressRepo, times(1)).save(progress);
    }

    @Test
    void getLessonProgress_returnsCorrectPercentage() {
        UUID userId = UUID.randomUUID();
        UserLessonProgress progress1 = new UserLessonProgress(userId, "mf-1");
        progress1.setStatus("COMPLETED");
        UserLessonProgress progress2 = new UserLessonProgress(userId, "mf-2");
        progress2.setStatus("IN_PROGRESS");

        when(lessonRepo.count()).thenReturn(4L);
        when(progressRepo.findByUserId(userId)).thenReturn(List.of(progress1, progress2));

        double pct = service.getLessonProgress(userId);
        assertThat(pct).isEqualTo(25.0); // 1 completed out of 4 total
    }

    @Test
    void getJargonTerm_whenNotFound_throwsResponseStatusException() {
        when(jargonRepo.findById("NAV")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getJargonTerm("NAV"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Jargon term not found");
    }
}

package in.sapphirus.rupee.learn.service;

import in.sapphirus.rupee.learn.domain.UserFeatures;
import in.sapphirus.rupee.learn.repo.UserFeaturesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StreakServiceTest {

    private final UserFeaturesRepository userFeaturesRepo = mock(UserFeaturesRepository.class);
    private StreakService streakService;

    @BeforeEach
    void setUp() {
        reset(userFeaturesRepo);
        streakService = new StreakService(userFeaturesRepo);
    }

    @Test
    void updateStreak_firstTimeUser_setsStreakToOne() {
        UUID userId = UUID.randomUUID();
        when(userFeaturesRepo.findById(userId)).thenReturn(Optional.empty());
        when(userFeaturesRepo.save(any(UserFeatures.class))).thenAnswer(i -> i.getArguments()[0]);

        StreakService.StreakView view = streakService.updateStreak(userId);

        assertThat(view.currentStreak()).isEqualTo(1);
        assertThat(view.maxStreak()).isEqualTo(1);
        assertThat(view.lastActiveAt()).isNotNull();
        verify(userFeaturesRepo, times(1)).save(any(UserFeatures.class));
    }

    @Test
    void updateStreak_consecutiveDayActivity_incrementsStreak() {
        UUID userId = UUID.randomUUID();
        UserFeatures uf = new UserFeatures(userId);
        uf.setStreakDaysCurrent(2);
        uf.setStreakDaysMax(2);
        uf.setLastActiveAt(Instant.now().minus(25, ChronoUnit.HOURS)); // 25 hours ago

        when(userFeaturesRepo.findById(userId)).thenReturn(Optional.of(uf));
        when(userFeaturesRepo.save(any(UserFeatures.class))).thenAnswer(i -> i.getArguments()[0]);

        StreakService.StreakView view = streakService.updateStreak(userId);

        assertThat(view.currentStreak()).isEqualTo(3);
        assertThat(view.maxStreak()).isEqualTo(3);
    }

    @Test
    void updateStreak_sameDayActivity_doesNotIncrementStreak() {
        UUID userId = UUID.randomUUID();
        UserFeatures uf = new UserFeatures(userId);
        uf.setStreakDaysCurrent(2);
        uf.setStreakDaysMax(5);
        uf.setLastActiveAt(Instant.now().minus(2, ChronoUnit.HOURS)); // 2 hours ago

        when(userFeaturesRepo.findById(userId)).thenReturn(Optional.of(uf));
        when(userFeaturesRepo.save(any(UserFeatures.class))).thenAnswer(i -> i.getArguments()[0]);

        StreakService.StreakView view = streakService.updateStreak(userId);

        assertThat(view.currentStreak()).isEqualTo(2);
        assertThat(view.maxStreak()).isEqualTo(5);
    }

    @Test
    void updateStreak_gapExceeds48Hours_resetsStreakToOne() {
        UUID userId = UUID.randomUUID();
        UserFeatures uf = new UserFeatures(userId);
        uf.setStreakDaysCurrent(10);
        uf.setStreakDaysMax(10);
        uf.setLastActiveAt(Instant.now().minus(50, ChronoUnit.HOURS)); // 50 hours ago

        when(userFeaturesRepo.findById(userId)).thenReturn(Optional.of(uf));
        when(userFeaturesRepo.save(any(UserFeatures.class))).thenAnswer(i -> i.getArguments()[0]);

        StreakService.StreakView view = streakService.updateStreak(userId);

        assertThat(view.currentStreak()).isEqualTo(1);
        assertThat(view.maxStreak()).isEqualTo(10); // Max streak preserved
    }

    @Test
    void getStreak_whenUserExists_returnsCurrentStreak() {
        UUID userId = UUID.randomUUID();
        UserFeatures uf = new UserFeatures(userId);
        uf.setStreakDaysCurrent(5);
        uf.setStreakDaysMax(7);

        when(userFeaturesRepo.findById(userId)).thenReturn(Optional.of(uf));

        StreakService.StreakView view = streakService.getStreak(userId);

        assertThat(view.currentStreak()).isEqualTo(5);
        assertThat(view.maxStreak()).isEqualTo(7);
    }

    @Test
    void resetExpiredStreaks_executesJobAndResetsStreaksOlderThan24Hours() {
        when(userFeaturesRepo.resetStreaksOlderThan(any(Instant.class))).thenReturn(4);

        streakService.resetExpiredStreaks();

        verify(userFeaturesRepo, times(1)).resetStreaksOlderThan(any(Instant.class));
    }
}

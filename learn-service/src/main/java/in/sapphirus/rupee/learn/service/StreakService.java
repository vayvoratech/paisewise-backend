package in.sapphirus.rupee.learn.service;

import in.sapphirus.rupee.learn.domain.UserFeatures;
import in.sapphirus.rupee.learn.repo.UserFeaturesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service managing user learning streaks and scheduled midnight reset jobs.
 * Enforces consecutive activity checks and 11:59 PM IST cron execution.
 */
@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);
    private final UserFeaturesRepository userFeaturesRepo;

    public StreakService(UserFeaturesRepository userFeaturesRepo) {
        this.userFeaturesRepo = userFeaturesRepo;
    }

    public record StreakView(int currentStreak, int maxStreak, Instant lastActiveAt) {}

    @Transactional
    public StreakView updateStreak(UUID userId) {
        UserFeatures uf = userFeaturesRepo.findById(userId)
                .orElseGet(() -> new UserFeatures(userId));

        Instant now = Instant.now();
        if (uf.getLastActiveAt() != null) {
            long hours = ChronoUnit.HOURS.between(uf.getLastActiveAt(), now);
            if (hours >= 24 && hours < 48) {
                // Active on consecutive day
                uf.setStreakDaysCurrent(uf.getStreakDaysCurrent() + 1);
                uf.setStreakDaysMax(Math.max(uf.getStreakDaysMax(), uf.getStreakDaysCurrent()));
            } else if (hours >= 48) {
                // Gap exceeded 48h -> Reset streak to 1
                uf.setStreakDaysCurrent(1);
            } else if (uf.getStreakDaysCurrent() == 0) {
                // First activity after reset
                uf.setStreakDaysCurrent(1);
                uf.setStreakDaysMax(Math.max(uf.getStreakDaysMax(), 1));
            }
        } else {
            uf.setStreakDaysCurrent(1);
            uf.setStreakDaysMax(1);
        }

        uf.setLastActiveAt(now);
        uf.setUpdatedAt(now);
        UserFeatures saved = userFeaturesRepo.save(uf);
        log.info("Updated streak for user {}: current={}, max={}", userId, saved.getStreakDaysCurrent(), saved.getStreakDaysMax());
        return new StreakView(saved.getStreakDaysCurrent(), saved.getStreakDaysMax(), saved.getLastActiveAt());
    }

    @Transactional(readOnly = true)
    public StreakView getStreak(UUID userId) {
        UserFeatures uf = userFeaturesRepo.findById(userId)
                .orElseGet(() -> new UserFeatures(userId));
        return new StreakView(uf.getStreakDaysCurrent(), uf.getStreakDaysMax(), uf.getLastActiveAt());
    }

    /**
     * Scheduled job executing daily at 11:59 PM IST (Asia/Kolkata).
     * Resets current streak to 0 for users inactive for > 24 hours.
     */
    @Scheduled(cron = "0 59 23 * * ?", zone = "Asia/Kolkata")
    @Transactional
    public void resetExpiredStreaks() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int resetCount = userFeaturesRepo.resetStreaksOlderThan(cutoff);
        log.info("Scheduled 11:59 PM IST Streak Reset completed. Expired streaks reset: {}", resetCount);
    }
}

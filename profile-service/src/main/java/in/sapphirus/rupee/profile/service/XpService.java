package in.sapphirus.rupee.profile.service;

import in.sapphirus.rupee.profile.domain.Profile;
import in.sapphirus.rupee.profile.domain.Badge;
import in.sapphirus.rupee.profile.repo.ProfileRepository;
import in.sapphirus.rupee.profile.repo.BadgeRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing user XP totals, level progression, and milestone badge awards.
 */
@Service
public class XpService {

    private final ProfileRepository profileRepo;
    private final BadgeRepository badgeRepo;
    private final KafkaTemplate<String, String> kafka;

    public XpService(ProfileRepository profileRepo, BadgeRepository badgeRepo, KafkaTemplate<String, String> kafka) {
        this.profileRepo = profileRepo;
        this.badgeRepo = badgeRepo;
        this.kafka = kafka;
    }

    @Transactional
    public void awardXp(UUID userId, int xpAmount, String source) {
        // Apply SELECT ... FOR UPDATE database lock to prevent concurrent double-rewards
        Profile p = profileRepo.findAndLockByUserId(userId.toString())
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        int oldXp = p.getXpTotal();
        int newXp = oldXp + xpAmount;
        p.setXpTotal(newXp);

        // Compute level threshold dynamically
        int oldLevel = p.getLevel();
        int newLevel = calculateLevel(newXp);

        if (newLevel > oldLevel) {
            p.setLevel(newLevel);
            checkAndProcessLevelUp(userId, newLevel);
        }

        profileRepo.save(p);
    }

    public int calculateLevel(int xp) {
        return (int) (Math.sqrt(xp) / 10) + 1;
    }

    private void checkAndProcessLevelUp(UUID userId, int newLevel) {
        // Award milestone badge
        if (newLevel == 5) {
            badgeRepo.save(new Badge(userId.toString(), "🎓", "Novice Scholar", "LEVEL_MILESTONE"));
        } else if (newLevel == 10) {
            badgeRepo.save(new Badge(userId.toString(), "🚀", "Market Master", "LEVEL_MILESTONE"));
        }

        // Broadcast Level-Up event asynchronously to Kafka topic 'user-events'
        try {
            String payload = String.format("{\"userId\":\"%s\",\"level\":%d,\"timestamp\":\"%s\"}",
                    userId, newLevel, Instant.now().toString());
            kafka.send("user-events", userId.toString(), payload);
        } catch (Exception e) {
            // Ignore broadcast failure in case broker is offline during local test execution
        }
    }
}

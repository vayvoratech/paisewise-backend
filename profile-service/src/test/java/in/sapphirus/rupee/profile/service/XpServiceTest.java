package in.sapphirus.rupee.profile.service;

import in.sapphirus.rupee.profile.domain.Profile;
import in.sapphirus.rupee.profile.domain.Badge;
import in.sapphirus.rupee.profile.repo.ProfileRepository;
import in.sapphirus.rupee.profile.repo.BadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class XpServiceTest {

    private final ProfileRepository profileRepo = mock(ProfileRepository.class);
    private final BadgeRepository badgeRepo = mock(BadgeRepository.class);
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private XpService service;

    @BeforeEach
    void setUp() {
        reset(profileRepo, badgeRepo, kafka);
        service = new XpService(profileRepo, badgeRepo, kafka);
    }

    @Test
    void awardXp_incrementsXpAndSavesProfile() {
        UUID userId = UUID.randomUUID();
        Profile p = new Profile(userId.toString(), "Test User", "@test", "Delhi");
        p.setXpTotal(10);
        p.setLevel(1);

        when(profileRepo.findAndLockByUserId(userId.toString())).thenReturn(Optional.of(p));
        when(profileRepo.save(any(Profile.class))).thenAnswer(i -> i.getArguments()[0]);

        service.awardXp(userId, 50, "QUIZ_COMPLETION");

        assertThat(p.getXpTotal()).isEqualTo(60);
        assertThat(p.getLevel()).isEqualTo(1); // sqrt(60)/10 + 1 = 7.7/10 + 1 = 1
        verify(profileRepo, times(1)).save(p);
        verifyNoInteractions(badgeRepo);
        verifyNoInteractions(kafka);
    }

    @Test
    void awardXp_triggersLevelUpAndSendsKafkaEventAndMilestoneBadges() {
        UUID userId = UUID.randomUUID();
        Profile p = new Profile(userId.toString(), "Test User", "@test", "Delhi");
        p.setXpTotal(90); // level = sqrt(90)/10 + 1 = 1.9 -> level 1
        p.setLevel(1);

        when(profileRepo.findAndLockByUserId(userId.toString())).thenReturn(Optional.of(p));
        when(profileRepo.save(any(Profile.class))).thenAnswer(i -> i.getArguments()[0]);

        // Add 160 XP -> Total XP = 250 -> level = sqrt(250)/10 + 1 = 15.8/10 + 1 = 2 -> levels up from 1 to 2
        service.awardXp(userId, 160, "LEVEL_UP_BOOST");

        assertThat(p.getXpTotal()).isEqualTo(250);
        assertThat(p.getLevel()).isEqualTo(2);
        verify(profileRepo, times(1)).save(p);
        
        // Verify level_up Kafka broadcast was triggered
        verify(kafka, times(1)).send(eq("user-events"), eq(userId.toString()), anyString());
    }

    @Test
    void awardXp_awardsMilestoneBadgeAtLevel5() {
        UUID userId = UUID.randomUUID();
        Profile p = new Profile(userId.toString(), "Test User", "@test", "Delhi");
        p.setXpTotal(1500); // level = sqrt(1500)/10 + 1 = 38.7/10 + 1 = 4
        p.setLevel(4);

        when(profileRepo.findAndLockByUserId(userId.toString())).thenReturn(Optional.of(p));
        when(profileRepo.save(any(Profile.class))).thenAnswer(i -> i.getArguments()[0]);

        // Add 100 XP -> Total XP = 1600 -> level = sqrt(1600)/10 + 1 = 40/10 + 1 = 5
        service.awardXp(userId, 100, "LEVEL_5_BOOST");

        assertThat(p.getLevel()).isEqualTo(5);
        
        // Verify badge saved
        ArgumentCaptor<Badge> badgeCaptor = ArgumentCaptor.forClass(Badge.class);
        verify(badgeRepo, times(1)).save(badgeCaptor.capture());
        
        Badge savedBadge = badgeCaptor.getValue();
        assertThat(savedBadge.getUserId()).isEqualTo(userId.toString());
        assertThat(savedBadge.getEmoji()).isEqualTo("🎓");
        assertThat(savedBadge.getTitle()).isEqualTo("Novice Scholar");
        assertThat(savedBadge.getCategory()).isEqualTo("LEVEL_MILESTONE");
    }
}

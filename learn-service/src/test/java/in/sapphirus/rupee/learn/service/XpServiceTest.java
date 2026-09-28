package in.sapphirus.rupee.learn.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class XpServiceTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private XpService xpService;

    @BeforeEach
    void setUp() {
        reset(kafkaTemplate);
        xpService = new XpService(kafkaTemplate);
    }

    @Test
    void awardXp_publishesPayloadToKafkaTopic() {
        UUID userId = UUID.randomUUID();
        xpService.awardXp(userId, 50, "TEST_REASON");

        verify(kafkaTemplate, times(1)).send(eq("xp-awards"), eq(userId.toString()), contains("50"));
    }

    @Test
    void awardXp_whenKafkaThrowsException_catchesAndLogsWithoutThrowing() {
        UUID userId = UUID.randomUUID();
        doThrow(new RuntimeException("Kafka Connection Failed")).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        // Should not throw exception upstream
        xpService.awardXp(userId, 50, "TEST_REASON");
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    void calculateLevel_returnsLevel1_forZeroXp() {
        int level = xpService.calculateLevel(0);
        assertThat(level).isEqualTo(1);
    }

    @Test
    void calculateLevel_returnsLevel1_for99Xp() {
        int level = xpService.calculateLevel(99);
        assertThat(level).isEqualTo(1);
    }

    @Test
    void calculateLevel_returnsLevel2_for100Xp() {
        int level = xpService.calculateLevel(100);
        assertThat(level).isEqualTo(2);
    }

    @Test
    void calculateLevel_returnsLevel1_forNegativeXp() {
        int level = xpService.calculateLevel(-50);
        assertThat(level).isEqualTo(1);
    }

    @Test
    void checkAndProcessLevelUp_whenLevelIncreases_returnsTrue() {
        // 90 XP (Lvl 1) -> 140 XP (Lvl 2)
        boolean leveledUp = xpService.checkAndProcessLevelUp(90, 140);
        assertThat(leveledUp).isTrue();
    }

    @Test
    void checkAndProcessLevelUp_whenLevelDoesNotIncrease_returnsFalse() {
        // 50 XP (Lvl 1) -> 90 XP (Lvl 1)
        boolean leveledUp = xpService.checkAndProcessLevelUp(50, 90);
        assertThat(leveledUp).isFalse();
    }
}

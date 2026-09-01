package in.sapphirus.rupee.learn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service class managing XP triggers. Publishes XP awards events asynchronously to Kafka.
 */
@Service
public class XpService {

    private static final Logger log = LoggerFactory.getLogger(XpService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public XpService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void awardXp(UUID userId, int xpAmount, String reason) {
        log.info("Triggering XP award: userId={}, xpAmount={}, reason={}", userId, xpAmount, reason);
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("xpAmount", xpAmount);
            payload.put("source", reason);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.info("Publishing XP award payload to Kafka topic 'xp-awards': {}", jsonPayload);
            kafkaTemplate.send("xp-awards", userId.toString(), jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish XP award to Kafka", e);
        }
    }
}

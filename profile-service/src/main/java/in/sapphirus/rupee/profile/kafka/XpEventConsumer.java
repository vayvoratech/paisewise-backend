package in.sapphirus.rupee.profile.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.profile.service.XpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumer listening to the 'xp-awards' topic to update profiles asynchronously.
 */
@Component
public class XpEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(XpEventConsumer.class);
    private final XpService xpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public XpEventConsumer(XpService xpService) {
        this.xpService = xpService;
    }

    @KafkaListener(topics = "xp-awards", groupId = "profile-xp-group")
    public void consumeXpAward(String message) {
        log.info("Received XP award event: {}", message);
        try {
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            
            String userIdStr = (String) event.get("userId");
            Number xpAmountNum = (Number) event.get("xpAmount");
            String source = (String) event.get("source");

            if (userIdStr != null && xpAmountNum != null) {
                UUID userId = UUID.fromString(userIdStr);
                int xpAmount = xpAmountNum.intValue();
                
                log.info("Delegating to XpService: userId={}, xpAmount={}, source={}", userId, xpAmount, source);
                xpService.awardXp(userId, xpAmount, source);
            } else {
                log.warn("XP award event contains missing fields: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to process XP award event", e);
        }
    }
}

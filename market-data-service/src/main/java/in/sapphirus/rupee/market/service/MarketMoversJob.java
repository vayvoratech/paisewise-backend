package in.sapphirus.rupee.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.market.dto.TopMoversDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MarketMoversJob {
    private static final Logger log = LoggerFactory.getLogger(MarketMoversJob.class);

    private final MarketDataService marketDataService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MarketMoversJob(MarketDataService marketDataService,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper) {
        this.marketDataService = marketDataService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs every 60 seconds (every minute) to refresh the top movers cache in Redis.
     */
    @Scheduled(fixedRate = 60000)
    public void refreshTopMovers() {
        log.info("Starting scheduled background refresh of Top Movers cache...");
        try {
            TopMoversDto movers = marketDataService.calculateTopMovers();
            String json = objectMapper.writeValueAsString(movers);
            redisTemplate.opsForValue().set("market:top-movers", json, Duration.ofMinutes(5));
            log.info("Successfully refreshed Top Movers cache in Redis.");
        } catch (Exception e) {
            log.error("Failed to refresh Top Movers cache: {}", e.getMessage(), e);
        }
    }
}

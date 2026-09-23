package in.sapphirus.rupee.practice.quote;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RedisQuoteService {
    private static final String KEY_PREFIX = "quote:";
    private final StringRedisTemplate redisTemplate;

    public RedisQuoteService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setQuote(String symbol, BigDecimal price) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + symbol,
                price.toPlainString()
        );
    }

    public BigDecimal getQuote(String symbol) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + symbol);

        if (value == null) {
            return null;
        }

        return new BigDecimal(value);
    }
}
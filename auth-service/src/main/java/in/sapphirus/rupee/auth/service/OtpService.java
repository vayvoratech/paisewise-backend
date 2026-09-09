package in.sapphirus.rupee.auth.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final StringRedisTemplate redis;
    private final PasswordEncoder encoder;
    private final in.sapphirus.rupee.auth.config.TwilioConfig twilioConfig;

    private boolean twilioInitialized = false;

    public OtpService(StringRedisTemplate redis, PasswordEncoder encoder, in.sapphirus.rupee.auth.config.TwilioConfig twilioConfig) {
        this.redis = redis;
        this.encoder = encoder;
        this.twilioConfig = twilioConfig;
    }

    @PostConstruct
    public void initTwilio() {
        String accountSid = twilioConfig.getAccountSid();
        String authToken = twilioConfig.getAuthToken();
        if (accountSid != null && !accountSid.isEmpty() && !accountSid.contains("ACxxxx")) {
            try {
                Twilio.init(accountSid, authToken);
                twilioInitialized = true;
                log.info("Twilio successfully initialized with account SID: {}", accountSid);
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage());
            }
        } else {
            log.warn("Twilio credentials are unset or placeholders. SMS sending will be simulated in logs.");
        }
    }

    public void sendOtp(String phone) {
        // 1. Enforce lockout logic: check if locked key exists
        if (Boolean.TRUE.equals(redis.hasKey("otp:lock:" + phone))) {
            throw new IllegalStateException("Account locked due to too many failed OTP attempts. Try again in 10 minutes.");
        }

        // Duplicate OTP resend window check (cooldown: 60s)
        Long expire = redis.getExpire("otp:code:" + phone);
        if (expire != null && expire > 60) {
            throw new IllegalStateException("Please wait before requesting another OTP.");
        }

        // 2. Generate and encrypt code
        String code = String.format("%06d", new Random().nextInt(1000000));
        String hash = encoder.encode(code);

        // 3. Save to Redis with 120s TTL
        redis.opsForValue().set("otp:code:" + phone, hash, Duration.ofSeconds(120));
        redis.opsForValue().set("otp:attempts:" + phone, "0", Duration.ofSeconds(120));

        log.info("DEBUG: Generated OTP for {}: {} (Hashed: {})", phone, code, hash);

        // 4. Send via Twilio SMS or log fallback
        String messageText = "Your PaiseWise OTP is: " + code + ". Valid for 120 seconds.";
        if (twilioInitialized) {
            try {
                Message.creator(
                        new PhoneNumber(phone),
                        new PhoneNumber(twilioConfig.getFromNumber()),
                        messageText
                ).create();
                log.info("OTP SMS dispatched successfully to {} via Twilio", phone);
            } catch (Exception e) {
                log.error("Twilio message dispatch failed: {}. Falling back to console logging.", e.getMessage());
            }
        } else {
            log.info("SMS SIMULATION LOG (TWILIO DISABLED): Send SMS to {} -> '{}'", phone, messageText);
        }
    }

    public boolean verifyOtp(String phone, String userInputCode) {
        String key = "otp:code:" + phone;
        String attemptsKey = "otp:attempts:" + phone;
        String lockKey = "otp:lock:" + phone;

        // Check if user is currently locked out
        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            throw new IllegalStateException("Account is currently locked out. Try again later.");
        }

        String hash = redis.opsForValue().get(key);
        if (hash == null) {
            return false;
        }

        if (encoder.matches(userInputCode, hash)) {
            redis.delete(key);
            redis.delete(attemptsKey);
            return true;
        } else {
            // Lockout count logic
            Long attempts = redis.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts >= 3) {
                redis.opsForValue().set(lockKey, "true", Duration.ofMinutes(10));
                redis.delete(key); // Invalidate active OTP on lockout
                throw new IllegalStateException("Account locked due to too many invalid attempts.");
            }
            return false;
        }
    }

    // --- MPIN Lockout Helpers ---

    public void checkMpinLockout(String phone) {
        if (Boolean.TRUE.equals(redis.hasKey("mpin:lock:" + phone))) {
            throw new IllegalStateException("Account locked due to too many failed MPIN attempts. Try again in 10 minutes.");
        }
    }

    public void trackMpinFailure(String phone) {
        String attemptsKey = "mpin:attempts:" + phone;
        String lockKey = "mpin:lock:" + phone;

        Long attempts = redis.opsForValue().increment(attemptsKey);
        if (attempts == null || attempts == 1L) {
            redis.expire(attemptsKey, Duration.ofMinutes(10));
        }

        if (attempts != null && attempts >= 5) {
            redis.opsForValue().set(lockKey, "true", Duration.ofMinutes(10));
            redis.delete(attemptsKey);
            throw new IllegalStateException("Account locked due to too many failed MPIN attempts.");
        }
    }

    public void resetMpinAttempts(String phone) {
        redis.delete("mpin:attempts:" + phone);
        redis.delete("mpin:lock:" + phone);
    }
}

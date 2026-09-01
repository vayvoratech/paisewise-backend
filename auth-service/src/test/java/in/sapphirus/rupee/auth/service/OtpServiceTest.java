package in.sapphirus.rupee.auth.service;

import in.sapphirus.rupee.auth.config.TwilioConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TwilioConfig twilioConfig = mock(TwilioConfig.class);
    
    private OtpService service;

    @BeforeEach
    void setUp() {
        reset(redis, ops, encoder, twilioConfig);
        when(redis.opsForValue()).thenReturn(ops);
        service = new OtpService(redis, encoder, twilioConfig);
    }

    @Test
    void initTwilio_withPlaceholderCredentials_doesNotInitializeTwilio() {
        when(twilioConfig.getAccountSid()).thenReturn("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        when(twilioConfig.getAuthToken()).thenReturn("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        
        service.initTwilio();
        
        verify(twilioConfig, times(1)).getAccountSid();
    }

    @Test
    void initTwilio_withNullCredentials_doesNotInitializeTwilio() {
        when(twilioConfig.getAccountSid()).thenReturn(null);
        
        service.initTwilio();
        
        verify(twilioConfig, times(1)).getAccountSid();
    }

    @Test
    void sendOtp_whenLocked_throwsIllegalStateException() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(true);

        assertThatThrownBy(() -> service.sendOtp(phone))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void sendOtp_whenNotLocked_generatesAndSavesHashedOtpToRedis() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(false);
        when(encoder.encode(any(CharSequence.class))).thenReturn("hashed_otp");

        service.sendOtp(phone);

        verify(ops, times(1)).set(eq("otp:code:" + phone), eq("hashed_otp"), any(Duration.class));
        verify(ops, times(1)).set(eq("otp:attempts:" + phone), eq("0"), any(Duration.class));
    }

    @Test
    void verifyOtp_whenLockoutActive_throwsIllegalStateException() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(true);

        assertThatThrownBy(() -> service.verifyOtp(phone, "123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked out");
    }

    @Test
    void verifyOtp_whenOtpNotFound_returnsFalse() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(false);
        when(ops.get("otp:code:" + phone)).thenReturn(null);

        boolean result = service.verifyOtp(phone, "123456");

        assertThat(result).isFalse();
    }

    @Test
    void verifyOtp_whenOtpMatches_clearsRedisAndReturnsTrue() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(false);
        when(ops.get("otp:code:" + phone)).thenReturn("hashed_otp");
        when(encoder.matches("123456", "hashed_otp")).thenReturn(true);

        boolean result = service.verifyOtp(phone, "123456");

        assertThat(result).isTrue();
        verify(redis, times(1)).delete("otp:code:" + phone);
        verify(redis, times(1)).delete("otp:attempts:" + phone);
    }

    @Test
    void verifyOtp_whenOtpMismatches_incrementsAttemptsAndReturnsFalse() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(false);
        when(ops.get("otp:code:" + phone)).thenReturn("hashed_otp");
        when(encoder.matches("123456", "hashed_otp")).thenReturn(false);
        when(ops.increment("otp:attempts:" + phone)).thenReturn(1L);

        boolean result = service.verifyOtp(phone, "123456");

        assertThat(result).isFalse();
        verify(ops, times(1)).increment("otp:attempts:" + phone);
    }

    @Test
    void verifyOtp_whenMaxAttemptsReached_locksAccountAndThrowsIllegalStateException() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(false);
        when(ops.get("otp:code:" + phone)).thenReturn("hashed_otp");
        when(encoder.matches("123456", "hashed_otp")).thenReturn(false);
        when(ops.increment("otp:attempts:" + phone)).thenReturn(3L);

        assertThatThrownBy(() -> service.verifyOtp(phone, "123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked");

        verify(ops, times(1)).set(eq("otp:lock:" + phone), eq("true"), any(Duration.class));
        verify(redis, times(1)).delete("otp:code:" + phone);
    }

    @Test
    void verifyOtp_withEmptyAttemptsResult_returnsFalseAndDoesNotLock() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(false);
        when(ops.get("otp:code:" + phone)).thenReturn("hashed_otp");
        when(encoder.matches("123456", "hashed_otp")).thenReturn(false);
        when(ops.increment("otp:attempts:" + phone)).thenReturn(null);

        boolean result = service.verifyOtp(phone, "123456");

        assertThat(result).isFalse();
        verify(ops, never()).set(eq("otp:lock:" + phone), anyString(), any(Duration.class));
    }

    @Test
    void sendOtp_whenWithinCooldownWindow_throwsIllegalStateException() {
        String phone = "9876543210";
        when(redis.hasKey("otp:lock:" + phone)).thenReturn(false);
        when(redis.getExpire("otp:code:" + phone)).thenReturn(90L); // TTL > 60s, requested recently

        assertThatThrownBy(() -> service.sendOtp(phone))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Please wait before requesting another OTP");
    }

    @Test
    void checkMpinLockout_whenLocked_throwsIllegalStateException() {
        String phone = "9876543210";
        when(redis.hasKey("mpin:lock:" + phone)).thenReturn(true);

        assertThatThrownBy(() -> service.checkMpinLockout(phone))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void trackMpinFailure_incrementsAndLocksAfterFiveFailures() {
        String phone = "9876543210";
        when(ops.increment("mpin:attempts:" + phone)).thenReturn(5L);

        assertThatThrownBy(() -> service.trackMpinFailure(phone))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked");

        verify(ops, times(1)).set(eq("mpin:lock:" + phone), eq("true"), any(Duration.class));
        verify(redis, times(1)).delete("mpin:attempts:" + phone);
    }
}

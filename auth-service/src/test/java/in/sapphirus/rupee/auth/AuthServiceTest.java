package in.sapphirus.rupee.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.auth.api.AuthDtos.*;
import in.sapphirus.rupee.auth.service.OtpService;
import in.sapphirus.rupee.auth.service.AuthService;
import org.springframework.transaction.annotation.Propagation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Public auth endpoints routed via MockMvc (local profile). */
@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AuthServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    @Autowired
    private in.sapphirus.rupee.security.JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Test
    void register_then_login_then_refresh() throws Exception {
        // 1. Register a new user
        RegisterRequest regReq = new RegisterRequest("9876543210", "Rahul", "rahul@example.com", "secret1", "secret1");
        String regResponseStr = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.name").value("Rahul"))
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokens.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        AuthResponse regResponse = objectMapper.readValue(regResponseStr, AuthResponse.class);

        // 2. Login
        LoginRequest loginReq = new LoginRequest("9876543210", "secret1");
        String loginResponseStr = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.phone").value("9876543210"))
                .andReturn().getResponse().getContentAsString();

        AuthResponse loginResponse = objectMapper.readValue(loginResponseStr, AuthResponse.class);

        // 3. Refresh token
        RefreshRequest refreshReq = new RefreshRequest(loginResponse.tokens().refreshToken());
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_with_wrong_password_fails() throws Exception {
        // Register user
        RegisterRequest regReq = new RegisterRequest("9000000001", "Test", "test1@example.com", "rightpass", "rightpass");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        // Login with wrong password -> expects 401 Unauthorized
        LoginRequest loginReq = new LoginRequest("9000000001", "wrongpass");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotated_refresh_token_cannot_be_reused() throws Exception {
        RegisterRequest regReq = new RegisterRequest("9000000002", "Test", "test2@example.com", "rightpass", "rightpass");
        String regResponseStr = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AuthResponse regResponse = objectMapper.readValue(regResponseStr, AuthResponse.class);
        String originalRefreshToken = regResponse.tokens().refreshToken();

        // First refresh -> rotates successfully
        RefreshRequest refreshReq = new RefreshRequest(originalRefreshToken);
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());

        // Second refresh with the same token -> must fail with 401 Unauthorized
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTokensAreRandom() {
        String t1 = jwtService.issueRefreshToken(UUID.randomUUID().toString(), "9876543210");
        String t2 = jwtService.issueRefreshToken(UUID.randomUUID().toString(), "9876543210");
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void full_auth_flow_send_otp_to_logout() throws Exception {
        String phone = "8888888888";
        String email = "flow@example.com";

        // 1. Send OTP (Simulated via OtpService mock)
        doNothing().when(otpService).sendOtp(phone);
        SendOtpRequest sendOtpReq = new SendOtpRequest(phone);
        mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendOtpReq)))
                .andExpect(status().isOk());

        // Register the user first so they can set MPIN and verify OTP (as verifyOtp requires a user lookup by email)
        RegisterRequest regReq = new RegisterRequest(phone, "Flow User", email, "secret123", "secret123");
        String regResponseStr = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AuthResponse regResponse = objectMapper.readValue(regResponseStr, AuthResponse.class);

        // 2. Verify OTP
        when(otpService.verifyOtp(eq(phone), eq("123456"))).thenReturn(true);
        VerifyOtpRequest verifyOtpReq = new VerifyOtpRequest(email, "123456");
        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpReq)))
                .andExpect(status().isOk());

        // 3. Set MPIN
        SetMpinRequest setMpinReq = new SetMpinRequest(email, "9999");
        mockMvc.perform(post("/auth/set-mpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setMpinReq)))
                .andExpect(status().isOk());

        // 4. Login with MPIN
        MpinLoginRequest mpinLoginReq = new MpinLoginRequest(phone, "9999");
        String mpinLoginResponseStr = mockMvc.perform(post("/auth/login/mpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mpinLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.phone").value(phone))
                .andReturn().getResponse().getContentAsString();

        AuthResponse mpinLoginResponse = objectMapper.readValue(mpinLoginResponseStr, AuthResponse.class);

        // 5. Logout using RefreshToken
        RefreshRequest logoutReq = new RefreshRequest(mpinLoginResponse.tokens().refreshToken());
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrent_refresh_requests_block_race_conditions() throws Exception {
        RegisterRequest regReq = new RegisterRequest("9555555555", "User", "con@test.com", "pass", "pass");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        AuthResponse regResponse = authService.login(new LoginRequest("9555555555", "pass"));
        String token = regResponse.tokens().refreshToken();

        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch endLatch = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.List<Tokens> successes = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize thread firing
                    successes.add(authService.refresh(token));
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown(); // Fire concurrently
        endLatch.await();
        executor.shutdown();

        // Assert only one request succeeded, while the second request was rejected
        assertThat(successes.size()).isEqualTo(1);
    }

    @Test
    void mpin_lockout_after_five_failures() throws Exception {
        String phone = "9000000005";
        String email = "lock@test.com";

        // Setup stateful mock lockout behavior for OtpService
        doNothing().when(otpService).checkMpinLockout(phone);
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger(0);
        doAnswer(invocation -> {
            int current = attempts.incrementAndGet();
            if (current >= 5) {
                doThrow(new IllegalStateException("Account locked due to too many failed MPIN attempts. Try again in 10 minutes."))
                        .when(otpService).checkMpinLockout(phone);
                throw new IllegalStateException("Account locked due to too many failed MPIN attempts.");
            }
            return null;
        }).when(otpService).trackMpinFailure(phone);

        // Register
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(phone, "LockUser", email, "secret123", "secret123"))))
                .andExpect(status().isCreated());
        
        // Set MPIN
        mockMvc.perform(post("/auth/set-mpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetMpinRequest(email, "9999"))))
                .andExpect(status().isOk());

        // Simulate 5 failed MPIN login attempts via mockMvc -> expects 401 Unauthorized
        MpinLoginRequest wrongReq = new MpinLoginRequest(phone, "1111");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login/mpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongReq)))
                    .andExpect(status().isUnauthorized());
        }

        // The 6th attempt with CORRECT MPIN must fail due to lock status (401 Unauthorized with Account Locked message)
        MpinLoginRequest correctReq = new MpinLoginRequest(phone, "9999");
        mockMvc.perform(post("/auth/login/mpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void refresh_after_logout_fails() throws Exception {
        String phone = "9000000006";
        String regResponseStr = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(phone, "LogoutUser", "logout@test.com", "secret123", "secret123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AuthResponse regResponse = objectMapper.readValue(regResponseStr, AuthResponse.class);
        String refreshToken = regResponse.tokens().refreshToken();

        // Logout
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk());

        // Try to refresh -> must return 401 Unauthorized
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
    }
}
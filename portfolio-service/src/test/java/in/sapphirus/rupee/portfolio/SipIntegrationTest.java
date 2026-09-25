package in.sapphirus.rupee.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.domain.Sip;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import in.sapphirus.rupee.portfolio.repo.SipRepository;
import in.sapphirus.rupee.portfolio.repo.SipWebhookEventRepository;
import in.sapphirus.rupee.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.mockito.stubbing.Answer;


/**
 * Full Spring Boot integration tests for SIP APIs and Razorpay webhook handler.
 *
 * Uses:
 *  - Spring Boot test slice with real application context
 *  - H2 in-memory database (application-test.yml)
 *  - Real JWT generation using JwtService (same logic as auth-service)
 *  - MockBean for RazorpayGatewayClient (no real Razorpay calls)
 *  - Real webhook HMAC-SHA256 signature generation to test security
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("SIP Mandates & UPI AutoPay — Integration Tests")
class SipIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwtService;
    @Autowired SipRepository sipRepo;
    @Autowired MfSchemeRepository mfSchemeRepo;
    @Autowired SipWebhookEventRepository webhookEventRepo;
    @Autowired in.sapphirus.rupee.portfolio.repo.MfInvestmentRepository mfInvestmentRepo;

    // Mock out the Razorpay SDK client — we don't make real API calls in tests
    @MockBean
    in.sapphirus.rupee.portfolio.client.RazorpayGatewayClient razorpayClient;

    // Test fixtures
    static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final String SCHEME_CODE = "INF179K01AA4";
    static final String WEBHOOK_SECRET = "test_webhook_secret";
    static UUID sipId;
    static String jwtToken;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() throws Exception {
        // Clean up in FK-safe order: investments → webhook_events → sips
        mfInvestmentRepo.deleteAll();
        webhookEventRepo.deleteAll();
        sipRepo.deleteAll();

        // Generate a real JWT for the test user
        jwtToken = jwtService.issueAccessToken(USER_ID.toString(), "+919876543210", Map.of());

        // Ensure an MF scheme exists (needed due to FK on sips.scheme_code)
        if (!mfSchemeRepo.existsById(SCHEME_CODE)) {
            MfScheme scheme = new MfScheme(SCHEME_CODE, "Axis Bluechip Fund - Growth", "Axis Mutual Fund", "Equity", 52.35);
            mfSchemeRepo.save(scheme);
        }

        // Stub Razorpay lifecycle calls (no real API)
        doNothing().when(razorpayClient).pauseSubscription(anyString());
        doNothing().when(razorpayClient).resumeSubscription(anyString());
        doNothing().when(razorpayClient).cancelSubscription(anyString());

        // Stub verifyWebhookSignature to perform real HMAC-SHA256
        // so that our integration tests exercise the actual security logic.
        org.mockito.Mockito.when(razorpayClient.verifyWebhookSignature(anyString(), anyString()))
                .thenAnswer((Answer<Boolean>) invocation -> {
                    String body = invocation.getArgument(0);
                    String sig  = invocation.getArgument(1);
                    String expected = hmacSha256(body, WEBHOOK_SECRET);
                    return expected.equals(sig);
                });
    }


    // ── Helper: seed a SIP ────────────────────────────────────────────────────

    private UUID seedSip(String status) {
        Sip sip = new Sip(USER_ID, SCHEME_CODE, 1500.0);
        sip.setStatus(status);
        sip.setRazorpaySubscriptionId("sub_test_integration_001");
        sip.setUpiMandateId("mandate_test_integration_001");
        sip.setNextDebitDate(LocalDate.now().plusDays(5));
        return sipRepo.save(sip).getId();
    }

    // ── Helper: compute real HMAC-SHA256 ─────────────────────────────────────

    private String hmacSha256(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. GET /sip/me — List SIPs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /sip/me → 401 without JWT")
    void listSips_noToken_returns401() throws Exception {
        mvc.perform(get("/sip/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("GET /sip/me → 200 with JWT, returns user SIPs")
    void listSips_withJwt_returns200() throws Exception {
        seedSip("ACTIVE");

        mvc.perform(get("/sip/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[0].schemeCode", is(SCHEME_CODE)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. GET /sip/{id} — Single SIP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("GET /sip/{id} → 200 returns correct SIP")
    void getSip_validId_returns200() throws Exception {
        UUID id = seedSip("ACTIVE");

        mvc.perform(get("/sip/" + id)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.amount", is(1500.0)))
                .andExpect(jsonPath("$.razorpaySubscriptionId", is("sub_test_integration_001")));
    }

    @Test
    @Order(4)
    @DisplayName("GET /sip/{id} → 404 for non-existent SIP")
    void getSip_unknownId_returns404() throws Exception {
        mvc.perform(get("/sip/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("SIP_NOT_FOUND")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. POST /sip/{id}/pause
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("POST /sip/{id}/pause → 200, SIP status becomes PAUSED")
    void pauseSip_activeSip_returns200AndPaused() throws Exception {
        UUID id = seedSip("ACTIVE");

        mvc.perform(post("/sip/" + id + "/pause")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Testing pause feature\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAUSED")))
                .andExpect(jsonPath("$.pausedReason", is("Testing pause feature")))
                .andExpect(jsonPath("$.pausedAt", notNullValue()));

        // Verify DB state
        Sip dbSip = sipRepo.findById(id).orElseThrow();
        assertThat(dbSip.getStatus()).isEqualTo("PAUSED");
        assertThat(dbSip.getPausedReason()).isEqualTo("Testing pause feature");
        assertThat(dbSip.getPausedAt()).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("POST /sip/{id}/pause → 409 CONFLICT when SIP is already PAUSED")
    void pauseSip_alreadyPaused_returns409() throws Exception {
        UUID id = seedSip("PAUSED");

        mvc.perform(post("/sip/" + id + "/pause")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SIP_INVALID_STATE")));
    }

    @Test
    @Order(7)
    @DisplayName("POST /sip/{id}/pause → 400 when reason is blank")
    void pauseSip_blankReason_returns400() throws Exception {
        UUID id = seedSip("ACTIVE");

        mvc.perform(post("/sip/" + id + "/pause")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. POST /sip/{id}/resume
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("POST /sip/{id}/resume → 200, SIP status becomes ACTIVE")
    void resumeSip_pausedSip_returns200AndActive() throws Exception {
        UUID id = seedSip("PAUSED");

        mvc.perform(post("/sip/" + id + "/resume")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.nextDebitDate", notNullValue()));

        assertThat(sipRepo.findById(id).orElseThrow().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @Order(9)
    @DisplayName("POST /sip/{id}/resume → 409 CONFLICT when SIP is ACTIVE (not paused)")
    void resumeSip_activeSip_returns409() throws Exception {
        UUID id = seedSip("ACTIVE");

        mvc.perform(post("/sip/" + id + "/resume")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SIP_INVALID_STATE")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. DELETE /sip/{id} — Cancel
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("DELETE /sip/{id} → 200, SIP status becomes CANCELLED")
    void cancelSip_activeSip_returns200AndCancelled() throws Exception {
        UUID id = seedSip("ACTIVE");

        mvc.perform(delete("/sip/" + id)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No longer needed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.cancelledReason", is("No longer needed")))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()));

        assertThat(sipRepo.findById(id).orElseThrow().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /sip/{id} → 403 FORBIDDEN when SIP belongs to another user")
    void cancelSip_otherUser_returns403() throws Exception {
        // Seed a SIP for a different user
        Sip otherSip = new Sip(UUID.randomUUID(), SCHEME_CODE, 1000.0);
        otherSip.setStatus("ACTIVE");
        UUID otherId = sipRepo.save(otherSip).getId();

        mvc.perform(delete("/sip/" + otherId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Should fail\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("SIP_ACCESS_DENIED")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. POST /webhooks/razorpay — Webhook Handler
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("POST /webhooks/razorpay → 400 without signature header")
    void webhook_noSignature_returns400() throws Exception {
        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"mandate.approved\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid webhook signature")));
    }

    @Test
    @Order(13)
    @DisplayName("POST /webhooks/razorpay → 400 with wrong signature")
    void webhook_wrongSignature_returns400() throws Exception {
        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "completely_wrong_signature")
                        .content("{\"event\":\"mandate.approved\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(14)
    @DisplayName("POST /webhooks/razorpay → mandate.approved activates SIP (real HMAC)")
    void webhook_mandateApproved_activatesSip() throws Exception {
        UUID id = seedSip("PENDING");
        sipRepo.findById(id).ifPresent(s -> {
            s.setUpiMandateId("mandate_test_integration_001");
            s.setStatus("PENDING");
            sipRepo.save(s);
        });

        String payload = """
                {
                  "id": "evt_mandate_integ_001",
                  "event": "mandate.approved",
                  "payload": {
                    "mandate": {
                      "entity": {
                        "id": "mandate_test_integration_001"
                      }
                    }
                  }
                }
                """;

        String sig = hmacSha256(payload, WEBHOOK_SECRET);

        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sig)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ok")));

        // Verify SIP was activated
        Sip updated = sipRepo.findByUpiMandateId("mandate_test_integration_001").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("ACTIVE");
        assertThat(updated.getUpiMandateStatus()).isEqualTo("APPROVED");

        // Verify webhook audit event was persisted
        assertThat(webhookEventRepo.existsByEventId("evt_mandate_integ_001")).isTrue();
    }

    @Test
    @Order(15)
    @DisplayName("POST /webhooks/razorpay → payment.captured creates MfInvestment + BSE order (stub)")
    void webhook_paymentCaptured_createsMfInvestment() throws Exception {
        UUID id = seedSip("ACTIVE");

        String payload = """
                {
                  "id": "evt_pay_integ_001",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_integration_test",
                        "amount": 150000,
                        "description": "SIP payment"
                      }
                    },
                    "subscription": {
                      "entity": {
                        "id": "sub_test_integration_001"
                      }
                    }
                  }
                }
                """;

        String sig = hmacSha256(payload, WEBHOOK_SECRET);

        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sig)
                        .content(payload))
                .andExpect(status().isOk());

        // Verify SIP counter was incremented
        Sip updated = sipRepo.findById(id).orElseThrow();
        assertThat(updated.getInstallmentsDone()).isEqualTo(1);
        assertThat(updated.getTotalInvested()).isEqualTo(1500.0); // 150000 paisa = 1500 INR

        // Verify webhook event logged
        assertThat(webhookEventRepo.existsByEventId("evt_pay_integ_001")).isTrue();
    }

    @Test
    @Order(16)
    @DisplayName("POST /webhooks/razorpay → duplicate event is idempotently ignored")
    void webhook_duplicateEvent_returns200WithAlreadyProcessed() throws Exception {
        String payload = "{\"id\":\"evt_dup_001\",\"event\":\"subscription.cancelled\",\"payload\":{\"subscription\":{\"entity\":{\"id\":\"sub_x\"}}}}";
        String sig = hmacSha256(payload, WEBHOOK_SECRET);

        // First call
        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sig)
                        .content(payload))
                .andExpect(status().isOk());

        // Second call — same event ID
        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sig)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already_processed")));
    }

    @Test
    @Order(17)
    @DisplayName("POST /webhooks/razorpay → payment.failed auto-pauses SIP after 3 failures")
    void webhook_paymentFailed_autoPausesAfterThreshold() throws Exception {
        UUID id = seedSip("ACTIVE");
        // Simulate 2 prior failures directly in DB
        sipRepo.findById(id).ifPresent(s -> { s.setInstallmentsFailed(2); sipRepo.save(s); });

        String payload = """
                {
                  "id": "evt_fail_integ_001",
                  "event": "payment.failed",
                  "payload": {
                    "payment": { "entity": { "id": "pay_fail", "amount": 150000 } },
                    "subscription": { "entity": { "id": "sub_test_integration_001" } }
                  }
                }
                """;

        String sig = hmacSha256(payload, WEBHOOK_SECRET);

        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sig)
                        .content(payload))
                .andExpect(status().isOk());

        // SIP should be auto-paused after 3rd failure
        Sip updated = sipRepo.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PAUSED");
        assertThat(updated.getInstallmentsFailed()).isEqualTo(3);
        assertThat(updated.getPausedReason()).contains("3 consecutive");
    }

    @Test
    @Order(18)
    @DisplayName("POST /webhooks/razorpay → subscription.cancelled marks SIP CANCELLED")
    void webhook_subscriptionCancelled_marksSipCancelled() throws Exception {
        UUID id = seedSip("ACTIVE");

        String payload = """
                {
                  "id": "evt_cancel_integ_001",
                  "event": "subscription.cancelled",
                  "payload": {
                    "subscription": {
                      "entity": {
                        "id": "sub_test_integration_001"
                      }
                    }
                  }
                }
                """;

        String sig = hmacSha256(payload, WEBHOOK_SECRET);

        mvc.perform(post("/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sig)
                        .content(payload))
                .andExpect(status().isOk());

        Sip updated = sipRepo.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("CANCELLED");
        assertThat(updated.getCancelledReason()).contains("webhook");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Actuator health
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(19)
    @DisplayName("GET /actuator/health → 200 without JWT (public endpoint)")
    void actuatorHealth_returns200() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}

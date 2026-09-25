package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.client.RazorpayOperations;
import in.sapphirus.rupee.portfolio.domain.SipWebhookEvent;
import in.sapphirus.rupee.portfolio.repo.SipWebhookEventRepository;
import in.sapphirus.rupee.portfolio.service.SipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RazorpayWebhookController")
class RazorpayWebhookControllerTest {

    @Mock RazorpayOperations razorpay;
    @Mock SipService sipService;
    @Mock SipWebhookEventRepository webhookEventRepo;

    RazorpayWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new RazorpayWebhookController(razorpay, sipService, webhookEventRepo);
    }

    // ── Signature verification ────────────────────────────────────────────────

    @Test
    @DisplayName("returns 400 when signature header is missing")
    void missingSignature_returns400() {
        ResponseEntity<String> response = controller.handleRazorpay("{}", null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("returns 400 when signature is invalid")
    void invalidSignature_returns400() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(false);

        ResponseEntity<String> response = controller.handleRazorpay("{\"event\":\"payment.captured\"}", "bad_sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns 200 with already_processed for duplicate event IDs")
    void duplicateEvent_returns200WithAlreadyProcessed() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepo.existsByEventId("evt_123")).thenReturn(true);

        String payload = "{\"id\":\"evt_123\",\"event\":\"payment.captured\"}";
        ResponseEntity<String> response = controller.handleRazorpay(payload, "valid_sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("already_processed");
        verifyNoInteractions(sipService);
    }

    // ── mandate.approved ──────────────────────────────────────────────────────

    @Test
    @DisplayName("mandate.approved routes to sipService.activateMandate")
    void mandateApproved_callsActivateMandate() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepo.existsByEventId(anyString())).thenReturn(false);
        when(sipService.activateMandate(anyString())).thenReturn(Optional.empty());
        when(webhookEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = """
                {
                  "id": "evt_mandate_001",
                  "event": "mandate.approved",
                  "payload": {
                    "mandate": {
                      "entity": {
                        "id": "mandate_abc"
                      }
                    }
                  }
                }
                """;

        ResponseEntity<String> response = controller.handleRazorpay(payload, "valid_sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(sipService).activateMandate("mandate_abc");
    }

    // ── payment.captured ──────────────────────────────────────────────────────

    @Test
    @DisplayName("payment.captured routes to sipService.handlePaymentCaptured")
    void paymentCaptured_callsHandlePaymentCaptured() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepo.existsByEventId(anyString())).thenReturn(false);
        when(sipService.handlePaymentCaptured(anyString(), anyDouble(), anyString()))
                .thenReturn(Optional.empty());
        when(webhookEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = """
                {
                  "id": "evt_pay_001",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_xyz",
                        "amount": 100000,
                        "description": "SIP payment"
                      }
                    },
                    "subscription": {
                      "entity": {
                        "id": "sub_001"
                      }
                    }
                  }
                }
                """;

        ResponseEntity<String> response = controller.handleRazorpay(payload, "valid_sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(sipService).handlePaymentCaptured("sub_001", 1000.0, "pay_xyz");
    }

    // ── payment.failed ────────────────────────────────────────────────────────

    @Test
    @DisplayName("payment.failed routes to sipService.handlePaymentFailed")
    void paymentFailed_callsHandlePaymentFailed() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepo.existsByEventId(anyString())).thenReturn(false);
        when(webhookEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = """
                {
                  "id": "evt_fail_001",
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_fail",
                        "amount": 100000
                      }
                    },
                    "subscription": {
                      "entity": {
                        "id": "sub_002"
                      }
                    }
                  }
                }
                """;

        ResponseEntity<String> response = controller.handleRazorpay(payload, "valid_sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(sipService).handlePaymentFailed("sub_002");
    }

    // ── subscription.cancelled ────────────────────────────────────────────────

    @Test
    @DisplayName("subscription.cancelled routes to sipService.handleSubscriptionCancelled")
    void subscriptionCancelled_callsHandleSubscriptionCancelled() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepo.existsByEventId(anyString())).thenReturn(false);
        when(webhookEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = """
                {
                  "id": "evt_cancel_001",
                  "event": "subscription.cancelled",
                  "payload": {
                    "subscription": {
                      "entity": {
                        "id": "sub_003"
                      }
                    }
                  }
                }
                """;

        ResponseEntity<String> response = controller.handleRazorpay(payload, "valid_sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(sipService).handleSubscriptionCancelled("sub_003");
    }

    // ── Audit log ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saves audit event for every processed webhook")
    void processedWebhook_savesAuditEvent() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepo.existsByEventId("evt_audit_001")).thenReturn(false);
        doNothing().when(sipService).handleSubscriptionCancelled(anyString());
        when(webhookEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));


        String payload = """
                {
                  "id": "evt_audit_001",
                  "event": "subscription.cancelled",
                  "payload": {
                    "subscription": { "entity": { "id": "sub_x" } }
                  }
                }
                """;

        controller.handleRazorpay(payload, "valid_sig");

        ArgumentCaptor<SipWebhookEvent> captor = ArgumentCaptor.forClass(SipWebhookEvent.class);
        verify(webhookEventRepo).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("evt_audit_001");
        assertThat(captor.getValue().getEventType()).isEqualTo("subscription.cancelled");
    }

    @Test
    @DisplayName("marks audit event as SKIPPED for unknown event types")
    void unknownEventType_markedAsSkipped() {
        when(razorpay.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(webhookEventRepo.existsByEventId(anyString())).thenReturn(false);
        when(webhookEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = "{\"id\":\"evt_unk\",\"event\":\"some.unknown.event\",\"payload\":{}}";

        controller.handleRazorpay(payload, "valid_sig");

        ArgumentCaptor<SipWebhookEvent> captor = ArgumentCaptor.forClass(SipWebhookEvent.class);
        verify(webhookEventRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SKIPPED");
    }
}

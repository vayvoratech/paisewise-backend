package in.sapphirus.rupee.portfolio.service;

import com.razorpay.RazorpayException;
import in.sapphirus.rupee.portfolio.client.BseStarMfClient;
import in.sapphirus.rupee.portfolio.client.RazorpayOperations;
import in.sapphirus.rupee.portfolio.domain.MfInvestment;
import in.sapphirus.rupee.portfolio.domain.Sip;
import in.sapphirus.rupee.portfolio.repo.MfInvestmentRepository;
import in.sapphirus.rupee.portfolio.repo.SipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SipService")
class SipServiceTest {

    @Mock SipRepository sipRepo;
    @Mock MfInvestmentRepository mfInvestmentRepo;
    @Mock RazorpayOperations razorpay;
    @Mock BseStarMfClient bseClient;

    SipService service;

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID OTHER_USER = UUID.randomUUID();
    private final UUID SIP_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SipService(sipRepo, mfInvestmentRepo, razorpay, bseClient);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Sip activeSip() {
        Sip sip = new Sip(USER_ID, "INF179K01AA4", 1000.0);
        sip.setStatus("ACTIVE");
        sip.setRazorpaySubscriptionId("sub_test_001");
        sip.setUpiMandateId("mandate_test_001");
        sip.setDebitDay(5);
        return sip;
    }

    // ── pauseSip ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("pauseSip")
    class PauseSipTests {

        @Test
        @DisplayName("pauses ACTIVE SIP and calls Razorpay subscription API")
        void pauseActiveSip_callsRazorpay() throws RazorpayException {
            Sip sip = activeSip();
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Sip result = service.pauseSip(SIP_ID, USER_ID, "Going on vacation");

            verify(razorpay).pauseSubscription("sub_test_001");
            assertThat(result.getStatus()).isEqualTo("PAUSED");
            assertThat(result.getPausedReason()).isEqualTo("Going on vacation");
            assertThat(result.getPausedAt()).isNotNull();
            assertThat(result.getUpiMandateStatus()).isEqualTo("PAUSED");
        }

        @Test
        @DisplayName("throws SipNotFoundException when SIP does not exist")
        void pauseNonExistentSip_throwsNotFound() {
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.pauseSip(SIP_ID, USER_ID, "reason"))
                    .isInstanceOf(SipService.SipNotFoundException.class);
        }

        @Test
        @DisplayName("throws SipAccessDeniedException when caller is not the owner")
        void pauseOtherUserSip_throwsForbidden() {
            Sip sip = activeSip(); // owned by USER_ID
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));

            assertThatThrownBy(() -> service.pauseSip(SIP_ID, OTHER_USER, "reason"))
                    .isInstanceOf(SipService.SipAccessDeniedException.class);
        }

        @Test
        @DisplayName("throws SipStateException when SIP is already PAUSED")
        void pauseAlreadyPausedSip_throwsStateException() {
            Sip sip = activeSip();
            sip.setStatus("PAUSED");
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));

            assertThatThrownBy(() -> service.pauseSip(SIP_ID, USER_ID, "reason"))
                    .isInstanceOf(SipService.SipStateException.class);
        }

        @Test
        @DisplayName("throws SipOperationException when Razorpay API fails")
        void pauseSip_razorpayFailure_throwsOperationException() throws RazorpayException {
            Sip sip = activeSip();
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));
            doThrow(new RazorpayException("API error")).when(razorpay).pauseSubscription(anyString());

            assertThatThrownBy(() -> service.pauseSip(SIP_ID, USER_ID, "reason"))
                    .isInstanceOf(SipService.SipOperationException.class)
                    .hasMessageContaining("Razorpay pause failed");
        }

        @Test
        @DisplayName("pauses SIP without Razorpay call when no subscriptionId (UPI mandate only)")
        void pauseSip_noSubscriptionId_skipsRazorpay() throws Exception {
            Sip sip = activeSip();
            sip.setRazorpaySubscriptionId(null);
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.pauseSip(SIP_ID, USER_ID, "manual pause");

            verifyNoInteractions(razorpay);
        }
    }

    // ── cancelSip ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelSip")
    class CancelSipTests {

        @Test
        @DisplayName("cancels ACTIVE SIP and calls Razorpay cancel API")
        void cancelActiveSip_callsRazorpay() throws RazorpayException {
            Sip sip = activeSip();
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Sip result = service.cancelSip(SIP_ID, USER_ID, "No longer needed");

            verify(razorpay).cancelSubscription("sub_test_001");
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(result.getCancelledReason()).isEqualTo("No longer needed");
            assertThat(result.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("cancels PAUSED SIP successfully")
        void cancelPausedSip_succeeds() throws RazorpayException {
            Sip sip = activeSip();
            sip.setStatus("PAUSED");
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Sip result = service.cancelSip(SIP_ID, USER_ID, "reason");

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("throws SipStateException when SIP is already CANCELLED")
        void cancelAlreadyCancelledSip_throwsStateException() {
            Sip sip = activeSip();
            sip.setStatus("CANCELLED");
            when(sipRepo.findById(SIP_ID)).thenReturn(Optional.of(sip));

            assertThatThrownBy(() -> service.cancelSip(SIP_ID, USER_ID, "reason"))
                    .isInstanceOf(SipService.SipStateException.class);
        }
    }

    // ── activateMandate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("activateMandate (mandate.approved webhook)")
    class ActivateMandateTests {

        @Test
        @DisplayName("sets SIP status to ACTIVE and mandate status to APPROVED")
        void activateMandate_setsSipActive() {
            Sip sip = activeSip();
            sip.setStatus("PENDING");
            when(sipRepo.findByUpiMandateId("mandate_001")).thenReturn(Optional.of(sip));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Optional<Sip> result = service.activateMandate("mandate_001");

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
            assertThat(result.get().getUpiMandateStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("returns empty Optional when mandateId is unknown")
        void activateMandate_unknownId_returnsEmpty() {
            when(sipRepo.findByUpiMandateId("unknown")).thenReturn(Optional.empty());

            Optional<Sip> result = service.activateMandate("unknown");

            assertThat(result).isEmpty();
        }
    }

    // ── handlePaymentCaptured ─────────────────────────────────────────────────

    @Nested
    @DisplayName("handlePaymentCaptured (payment.captured webhook)")
    class HandlePaymentCapturedTests {

        @Test
        @DisplayName("creates MfInvestment and submits BSE purchase")
        void handlePaymentCaptured_createsMfInvestmentAndCallsBse() {
            Sip sip = activeSip();
            when(sipRepo.findByRazorpaySubscriptionId("sub_test_001")).thenReturn(Optional.of(sip));
            when(mfInvestmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bseClient.submitPurchase(any(), any(), anyDouble(), any(), any()))
                    .thenReturn("BSE_ORDER_123");
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Optional<MfInvestment> result = service.handlePaymentCaptured("sub_test_001", 1000.0, "pay_abc");

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo("SUBMITTED");
            assertThat(result.get().getBseOrderId()).isEqualTo("BSE_ORDER_123");

            // Verify SIP counters incremented
            ArgumentCaptor<Sip> sipCaptor = ArgumentCaptor.forClass(Sip.class);
            verify(sipRepo).save(sipCaptor.capture());
            assertThat(sipCaptor.getValue().getInstallmentsDone()).isEqualTo(1);
            assertThat(sipCaptor.getValue().getTotalInvested()).isEqualTo(1000.0);
        }

        @Test
        @DisplayName("records FAILED investment when BSE call throws")
        void handlePaymentCaptured_bseFailure_marksFailed() {
            Sip sip = activeSip();
            when(sipRepo.findByRazorpaySubscriptionId("sub_test_001")).thenReturn(Optional.of(sip));
            when(mfInvestmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(bseClient.submitPurchase(any(), any(), anyDouble(), any(), any()))
                    .thenThrow(new RuntimeException("BSE unavailable"));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Optional<MfInvestment> result = service.handlePaymentCaptured("sub_test_001", 1000.0, "pay_abc");

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("returns empty Optional when subscriptionId is unknown")
        void handlePaymentCaptured_unknownSub_returnsEmpty() {
            when(sipRepo.findByRazorpaySubscriptionId("unknown")).thenReturn(Optional.empty());

            Optional<MfInvestment> result = service.handlePaymentCaptured("unknown", 1000.0, "pay_xyz");

            assertThat(result).isEmpty();
        }
    }

    // ── handlePaymentFailed ───────────────────────────────────────────────────

    @Nested
    @DisplayName("handlePaymentFailed (payment.failed webhook)")
    class HandlePaymentFailedTests {

        @Test
        @DisplayName("increments failure counter without auto-pausing below threshold")
        void handlePaymentFailed_incrementsCounter() {
            Sip sip = activeSip();
            sip.setInstallmentsFailed(1);
            when(sipRepo.findByRazorpaySubscriptionId("sub_test_001")).thenReturn(Optional.of(sip));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.handlePaymentFailed("sub_test_001");

            ArgumentCaptor<Sip> captor = ArgumentCaptor.forClass(Sip.class);
            verify(sipRepo).save(captor.capture());
            assertThat(captor.getValue().getInstallmentsFailed()).isEqualTo(2);
            assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE"); // not paused yet
        }

        @Test
        @DisplayName("auto-pauses SIP after 3 consecutive failures")
        void handlePaymentFailed_autoPausesAtThreshold() {
            Sip sip = activeSip();
            sip.setInstallmentsFailed(2); // third failure will hit threshold
            when(sipRepo.findByRazorpaySubscriptionId("sub_test_001")).thenReturn(Optional.of(sip));
            when(sipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.handlePaymentFailed("sub_test_001");

            ArgumentCaptor<Sip> captor = ArgumentCaptor.forClass(Sip.class);
            verify(sipRepo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("PAUSED");
            assertThat(captor.getValue().getPausedReason()).contains("3 consecutive");
        }
    }
}

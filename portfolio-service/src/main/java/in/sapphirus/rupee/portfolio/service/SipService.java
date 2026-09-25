package in.sapphirus.rupee.portfolio.service;

import com.razorpay.RazorpayException;
import in.sapphirus.rupee.portfolio.client.BseStarMfClient;
import in.sapphirus.rupee.portfolio.client.RazorpayOperations;
import in.sapphirus.rupee.portfolio.domain.MfInvestment;
import in.sapphirus.rupee.portfolio.domain.Sip;
import in.sapphirus.rupee.portfolio.repo.MfInvestmentRepository;
import in.sapphirus.rupee.portfolio.repo.SipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for SIP lifecycle management.
 *
 * <h3>Status transitions</h3>
 * <pre>
 * ACTIVE  ──pause──►  PAUSED  ──resume──►  ACTIVE
 * ACTIVE  ──cancel──► CANCELLED
 * PAUSED  ──cancel──► CANCELLED
 * </pre>
 *
 * <h3>Razorpay integration</h3>
 * Each lifecycle change calls the corresponding Razorpay Subscription API
 * (pause / resume / cancel). If the SIP has no {@code razorpaySubscriptionId}
 * (UPI mandate flow only) the Razorpay call is skipped and only the DB is updated.
 */
@Service
public class SipService {

    private static final Logger log = LoggerFactory.getLogger(SipService.class);

    // How many consecutive payment failures before auto-pausing a SIP
    private static final int AUTO_PAUSE_THRESHOLD = 3;

    private final SipRepository sipRepo;
    private final MfInvestmentRepository mfInvestmentRepo;
    private final RazorpayOperations razorpay;
    private final BseStarMfClient bseClient;

    public SipService(SipRepository sipRepo,
                      MfInvestmentRepository mfInvestmentRepo,
                      RazorpayOperations razorpay,
                      BseStarMfClient bseClient) {
        this.sipRepo = sipRepo;
        this.mfInvestmentRepo = mfInvestmentRepo;
        this.razorpay = razorpay;
        this.bseClient = bseClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /** List all SIPs belonging to the authenticated user. */
    public List<Sip> listMySips(UUID userId) {
        return sipRepo.findByUserIdOrderByStartDateDesc(userId);
    }

    /**
     * Fetch a single SIP by ID, verifying ownership.
     *
     * @throws SipNotFoundException   if no SIP with that ID exists
     * @throws SipAccessDeniedException if the SIP belongs to a different user
     */
    public Sip getSip(UUID sipId, UUID userId) {
        Sip sip = sipRepo.findById(sipId)
                .orElseThrow(() -> new SipNotFoundException(sipId));
        requireOwner(sip, userId);
        return sip;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle mutations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pause an active SIP.
     *
     * @param sipId  target SIP
     * @param userId caller's user ID (ownership check)
     * @param reason human-readable pause reason stored in DB
     * @throws SipNotFoundException      if the SIP does not exist
     * @throws SipAccessDeniedException  if the caller does not own the SIP
     * @throws SipStateException         if the SIP is not in ACTIVE state
     * @throws SipOperationException     if the Razorpay API call fails
     */
    @Transactional
    public Sip pauseSip(UUID sipId, UUID userId, String reason) {
        Sip sip = getSip(sipId, userId);
        requireStatus(sip, "ACTIVE");

        // Call Razorpay first (subscription flow)
        if (sip.getRazorpaySubscriptionId() != null) {
            try {
                razorpay.pauseSubscription(sip.getRazorpaySubscriptionId());
            } catch (RazorpayException e) {
                log.error("Razorpay pause failed for sub={}: {}", sip.getRazorpaySubscriptionId(), e.getMessage());
                throw new SipOperationException("Razorpay pause failed: " + e.getMessage(), e);
            }
        }

        sip.setStatus("PAUSED");
        sip.setPausedAt(Instant.now());
        sip.setPausedReason(reason);
        sip.setUpdatedAt(Instant.now());

        // UPI mandate status update
        if (sip.getUpiMandateId() != null) {
            sip.setUpiMandateStatus("PAUSED");
        }

        Sip saved = sipRepo.save(sip);
        log.info("SIP {} paused by user {} — reason: {}", sipId, userId, reason);
        return saved;
    }

    /**
     * Resume a paused SIP.
     *
     * @throws SipStateException if the SIP is not in PAUSED state
     */
    @Transactional
    public Sip resumeSip(UUID sipId, UUID userId) {
        Sip sip = getSip(sipId, userId);
        requireStatus(sip, "PAUSED");

        // Call Razorpay first (subscription flow)
        if (sip.getRazorpaySubscriptionId() != null) {
            try {
                razorpay.resumeSubscription(sip.getRazorpaySubscriptionId());
            } catch (RazorpayException e) {
                log.error("Razorpay resume failed for sub={}: {}", sip.getRazorpaySubscriptionId(), e.getMessage());
                throw new SipOperationException("Razorpay resume failed: " + e.getMessage(), e);
            }
        }

        sip.setStatus("ACTIVE");
        // Recalculate next debit date from today
        sip.setNextDebitDate(nextDebitDate(sip.getDebitDay()));
        sip.setUpdatedAt(Instant.now());

        // Clear pause metadata
        sip.setPausedReason(null);

        if (sip.getUpiMandateId() != null) {
            sip.setUpiMandateStatus("APPROVED");
        }

        Sip saved = sipRepo.save(sip);
        log.info("SIP {} resumed by user {}", sipId, userId);
        return saved;
    }

    /**
     * Cancel a SIP (irreversible).
     *
     * @throws SipStateException if the SIP is already CANCELLED or COMPLETED
     */
    @Transactional
    public Sip cancelSip(UUID sipId, UUID userId, String reason) {
        Sip sip = getSip(sipId, userId);
        if ("CANCELLED".equals(sip.getStatus()) || "COMPLETED".equals(sip.getStatus())) {
            throw new SipStateException("SIP " + sipId + " is already " + sip.getStatus());
        }

        // Call Razorpay first (subscription flow)
        if (sip.getRazorpaySubscriptionId() != null) {
            try {
                razorpay.cancelSubscription(sip.getRazorpaySubscriptionId());
            } catch (RazorpayException e) {
                log.error("Razorpay cancel failed for sub={}: {}", sip.getRazorpaySubscriptionId(), e.getMessage());
                throw new SipOperationException("Razorpay cancel failed: " + e.getMessage(), e);
            }
        }

        sip.setStatus("CANCELLED");
        sip.setCancelledAt(Instant.now());
        sip.setCancelledReason(reason);
        sip.setUpdatedAt(Instant.now());

        if (sip.getUpiMandateId() != null) {
            sip.setUpiMandateStatus("CANCELLED");
        }

        Sip saved = sipRepo.save(sip);
        log.info("SIP {} cancelled by user {} — reason: {}", sipId, userId, reason);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Webhook-driven operations (called from RazorpayWebhookController)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when Razorpay fires a {@code mandate.approved} webhook.
     * Activates the SIP and marks the UPI mandate as APPROVED.
     *
     * @param mandateId Razorpay UPI mandate ID
     * @return the updated SIP, or empty if no SIP matches the mandate ID
     */
    @Transactional
    public Optional<Sip> activateMandate(String mandateId) {
        Optional<Sip> opt = sipRepo.findByUpiMandateId(mandateId);
        opt.ifPresent(sip -> {
            sip.setUpiMandateStatus("APPROVED");
            sip.setStatus("ACTIVE");
            sip.setUpdatedAt(Instant.now());
            sipRepo.save(sip);
            log.info("Mandate {} approved — SIP {} set ACTIVE", mandateId, sip.getId());
        });
        if (opt.isEmpty()) {
            log.warn("mandate.approved received for unknown mandateId={}", mandateId);
        }
        return opt;
    }

    /**
     * Called when Razorpay fires a {@code payment.captured} webhook (subscription debit succeeded).
     *
     * <ol>
     *   <li>Resolves the SIP by {@code subscriptionId} (or mandate ID as fallback)</li>
     *   <li>Creates a {@code MfInvestment} record in SUBMITTED state</li>
     *   <li>Calls {@link BseStarMfClient#submitPurchase} to place the BSE order</li>
     *   <li>Updates the SIP counters and next debit date</li>
     * </ol>
     *
     * @param subscriptionId Razorpay subscription ID from the payment event
     * @param amount         debited amount in INR
     * @param razorpayPaymentId Razorpay payment ID (stored as reference)
     */
    @Transactional
    public Optional<MfInvestment> handlePaymentCaptured(String subscriptionId,
                                                         double amount,
                                                         String razorpayPaymentId) {
        Optional<Sip> sipOpt = sipRepo.findByRazorpaySubscriptionId(subscriptionId);
        if (sipOpt.isEmpty()) {
            log.warn("payment.captured received for unknown subscriptionId={}", subscriptionId);
            return Optional.empty();
        }

        Sip sip = sipOpt.get();

        // 1. Create MfInvestment record (PENDING → will be updated on BSE response)
        MfInvestment inv = new MfInvestment(sip.getUserId(), sip.getSchemeCode(), amount);
        inv.setSipId(sip.getId());
        inv.setTransactionType("SIP");
        inv.setStatus("PENDING");
        inv.setBseOrderId(razorpayPaymentId); // temp ref; overwritten after BSE call

        MfInvestment saved = mfInvestmentRepo.save(inv);

        // 2. Submit purchase to BSE StarMF
        try {
            String bseOrderId = bseClient.submitPurchase(
                    sip.getUserId(),
                    sip.getSchemeCode(),
                    amount,
                    saved.getFolioNumber(),
                    sip.getId()
            );
            saved.setBseOrderId(bseOrderId);
            saved.setStatus("SUBMITTED");
            mfInvestmentRepo.save(saved);
            log.info("BSE purchase submitted: sipId={}, bseOrderId={}, amount={}", sip.getId(), bseOrderId, amount);
        } catch (Exception e) {
            log.error("BSE purchase failed for sipId={}: {}", sip.getId(), e.getMessage());
            saved.setStatus("FAILED");
            mfInvestmentRepo.save(saved);
        }

        // 3. Update SIP counters
        sip.setInstallmentsDone(sip.getInstallmentsDone() + 1);
        sip.setTotalInvested(sip.getTotalInvested() + amount);
        sip.setNextDebitDate(nextDebitDate(sip.getDebitDay()));
        sip.setUpdatedAt(Instant.now());

        // Auto-complete if all planned installments done
        if (sip.getInstallmentsPlanned() > 0
                && sip.getInstallmentsDone() >= sip.getInstallmentsPlanned()) {
            sip.setStatus("COMPLETED");
            log.info("SIP {} completed all {} installments", sip.getId(), sip.getInstallmentsPlanned());
        }
        sipRepo.save(sip);

        return Optional.of(saved);
    }

    /**
     * Called when Razorpay fires a {@code payment.failed} webhook.
     * Increments the failure counter; auto-pauses the SIP after {@link #AUTO_PAUSE_THRESHOLD}.
     *
     * @param subscriptionId Razorpay subscription ID
     */
    @Transactional
    public void handlePaymentFailed(String subscriptionId) {
        sipRepo.findByRazorpaySubscriptionId(subscriptionId).ifPresent(sip -> {
            int failures = sip.getInstallmentsFailed() + 1;
            sip.setInstallmentsFailed(failures);
            sip.setUpdatedAt(Instant.now());

            if (failures >= AUTO_PAUSE_THRESHOLD && "ACTIVE".equals(sip.getStatus())) {
                sip.setStatus("PAUSED");
                sip.setPausedAt(Instant.now());
                sip.setPausedReason("Auto-paused after " + failures + " consecutive payment failures");
                log.warn("SIP {} auto-paused after {} payment failures", sip.getId(), failures);
            }

            sipRepo.save(sip);
        });
    }

    /**
     * Called when Razorpay fires a {@code subscription.cancelled} webhook.
     * Marks the SIP as CANCELLED if it isn't already.
     */
    @Transactional
    public void handleSubscriptionCancelled(String subscriptionId) {
        sipRepo.findByRazorpaySubscriptionId(subscriptionId).ifPresent(sip -> {
            if (!"CANCELLED".equals(sip.getStatus())) {
                sip.setStatus("CANCELLED");
                sip.setCancelledAt(Instant.now());
                sip.setCancelledReason("Cancelled via Razorpay webhook");
                sip.setUpdatedAt(Instant.now());
                sipRepo.save(sip);
                log.info("SIP {} marked CANCELLED via webhook (subscriptionId={})", sip.getId(), subscriptionId);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void requireOwner(Sip sip, UUID userId) {
        if (!sip.getUserId().equals(userId)) {
            throw new SipAccessDeniedException("SIP " + sip.getId() + " does not belong to user " + userId);
        }
    }

    private void requireStatus(Sip sip, String requiredStatus) {
        if (!requiredStatus.equals(sip.getStatus())) {
            throw new SipStateException(
                    "SIP " + sip.getId() + " must be " + requiredStatus + " but is " + sip.getStatus());
        }
    }

    /**
     * Compute the next debit date from today for the given debit day of month.
     * If the debit day has already passed this month, schedule for next month.
     */
    private LocalDate nextDebitDate(int debitDay) {
        LocalDate today = LocalDate.now();
        LocalDate candidate = today.withDayOfMonth(Math.min(debitDay, today.lengthOfMonth()));
        return candidate.isAfter(today) ? candidate : candidate.plusMonths(1)
                .withDayOfMonth(Math.min(debitDay, candidate.plusMonths(1).lengthOfMonth()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Domain exceptions (package-private; translated to HTTP in controller)
    // ─────────────────────────────────────────────────────────────────────────

    public static class SipNotFoundException extends RuntimeException {
        public SipNotFoundException(UUID id) {
            super("SIP not found: " + id);
        }
    }

    public static class SipAccessDeniedException extends RuntimeException {
        public SipAccessDeniedException(String msg) { super(msg); }
    }

    public static class SipStateException extends RuntimeException {
        public SipStateException(String msg) { super(msg); }
    }

    public static class SipOperationException extends RuntimeException {
        public SipOperationException(String msg, Throwable cause) { super(msg, cause); }
    }
}

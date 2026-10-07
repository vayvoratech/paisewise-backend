package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.client.RazorpayOperations;
import in.sapphirus.rupee.portfolio.domain.SipWebhookEvent;
import in.sapphirus.rupee.portfolio.repo.SipWebhookEventRepository;
import in.sapphirus.rupee.portfolio.service.SipService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Razorpay Webhook Handler.
 *
 * <h3>Security</h3>
 * This endpoint is PUBLIC (no JWT) — Razorpay cannot attach user tokens.
 * Authenticity is enforced via HMAC-SHA256 signature verification using the
 * shared webhook secret configured in Razorpay Dashboard → Webhooks.
 * Any request with an invalid or missing signature is rejected with HTTP 400.
 * The security filter chain must permit {@code /webhooks/**} without authentication.
 *
 * <h3>Idempotency</h3>
 * Razorpay retries webhook delivery on 5xx responses. Each event is stored in
 * {@code sip_webhook_events} with the Razorpay event ID as a UNIQUE key.
 * Duplicate deliveries are detected and responded with HTTP 200 (to stop retries).
 *
 * <h3>Handled events</h3>
 * <ul>
 *   <li>{@code mandate.approved}       — activates SIP and UPI mandate</li>
 *   <li>{@code payment.captured}       — triggers BSE StarMF purchase on successful debit</li>
 *   <li>{@code payment.failed}         — increments failure counter, auto-pauses after threshold</li>
 *   <li>{@code subscription.cancelled} — marks SIP as CANCELLED</li>
 * </ul>
 *
 * Unhandled event types are logged and acknowledged with 200 to prevent retries.
 */
@RestController
@RequestMapping("/webhooks")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayOperations razorpay;
    private final SipService sipService;
    private final SipWebhookEventRepository webhookEventRepo;

    public RazorpayWebhookController(RazorpayOperations razorpay,
                                      SipService sipService,
                                      SipWebhookEventRepository webhookEventRepo) {
        this.razorpay = razorpay;
        this.sipService = sipService;
        this.webhookEventRepo = webhookEventRepo;
    }

    /**
     * Main webhook receiver.
     *
     * @param rawBody   raw request body as string — MUST be read before any JSON parsing
     *                  to ensure HMAC is computed on the exact bytes Razorpay sent
     * @param signature value of {@code X-Razorpay-Signature} header
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpay(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        // ── 1. Signature verification ─────────────────────────────────────────
        if (signature == null || !razorpay.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Razorpay webhook rejected — invalid or missing signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Invalid webhook signature\"}");
        }

        // ── 2. Parse payload ──────────────────────────────────────────────────
        JSONObject payload;
        String eventId;
        String eventType;
        try {
            payload = new JSONObject(rawBody);
            eventId = payload.optString("id", "unknown_" + System.currentTimeMillis());
            eventType = payload.optString("event", "unknown");
        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Malformed JSON\"}");
        }

        // ── 3. Idempotency check ──────────────────────────────────────────────
        if (webhookEventRepo.existsByEventId(eventId)) {
            log.info("Duplicate webhook eventId={} skipped", eventId);
            return ResponseEntity.ok("{\"status\":\"already_processed\"}");
        }

        // ── 4. Audit log entry ────────────────────────────────────────────────
        SipWebhookEvent auditEvent = new SipWebhookEvent(eventId, eventType, rawBody);

        // ── 5. Route by event type ────────────────────────────────────────────
        try {
            switch (eventType) {

                case "mandate.approved" -> {
                    String mandateId = extractMandateId(payload);
                    log.info("Processing mandate.approved: mandateId={}", mandateId);
                    sipService.activateMandate(mandateId)
                            .ifPresent(sip -> auditEvent.setSipId(sip.getId()));
                }

                case "payment.captured" -> {
                    String subscriptionId = extractSubscriptionId(payload);
                    double amount = extractAmount(payload);
                    String paymentId = extractPaymentId(payload);
                    log.info("Processing payment.captured: sub={}, amount={}, payment={}", subscriptionId, amount, paymentId);
                    sipService.handlePaymentCaptured(subscriptionId, amount, paymentId)
                            .ifPresent(inv -> auditEvent.setSipId(inv.getSipId()));
                }

                case "payment.failed" -> {
                    String subscriptionId = extractSubscriptionId(payload);
                    log.info("Processing payment.failed: sub={}", subscriptionId);
                    sipService.handlePaymentFailed(subscriptionId);
                }

                case "subscription.cancelled" -> {
                    String subscriptionId = extractSubscriptionIdFromSub(payload);
                    log.info("Processing subscription.cancelled: sub={}", subscriptionId);
                    sipService.handleSubscriptionCancelled(subscriptionId);
                }

                default -> {
                    log.debug("Unhandled Razorpay event type: {}", eventType);
                    auditEvent.setStatus("SKIPPED");
                }
            }

        } catch (Exception e) {
            log.error("Error processing Razorpay event eventId={}, type={}: {}", eventId, eventType, e.getMessage(), e);
            auditEvent.setStatus("ERROR");
            auditEvent.setErrorMessage(e.getMessage());
        }

        // ── 6. Persist audit record ───────────────────────────────────────────
        try {
            webhookEventRepo.save(auditEvent);
        } catch (DataIntegrityViolationException e) {
            // Race condition — another thread already processed this event ID
            log.info("Concurrent duplicate webhook eventId={} ignored", eventId);
        }

        // Always return 200 so Razorpay stops retrying
        return ResponseEntity.ok("{\"status\":\"ok\"}");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payload extraction helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extract UPI mandate ID from a mandate.approved event.
     * Razorpay payload path: payload.mandate.entity.id
     */
    private String extractMandateId(JSONObject root) {
        return root.getJSONObject("payload")
                .getJSONObject("mandate")
                .getJSONObject("entity")
                .getString("id");
    }

    /**
     * Extract Razorpay subscription ID from a payment.captured / payment.failed event.
     * Razorpay payload path: payload.payment.entity.description (or subscription_id)
     * For subscription payments: payload.subscription.entity.id is preferred when present.
     */
    private String extractSubscriptionId(JSONObject root) {
        JSONObject payloadObj = root.getJSONObject("payload");
        // Prefer subscription entity if present
        if (payloadObj.has("subscription")) {
            return payloadObj.getJSONObject("subscription")
                    .getJSONObject("entity")
                    .getString("id");
        }
        // Fallback: read from payment entity description / notes
        return payloadObj.getJSONObject("payment")
                .getJSONObject("entity")
                .optString("description", "");
    }

    /**
     * Extract Razorpay subscription ID from a subscription.cancelled event.
     * Razorpay payload path: payload.subscription.entity.id
     */
    private String extractSubscriptionIdFromSub(JSONObject root) {
        return root.getJSONObject("payload")
                .getJSONObject("subscription")
                .getJSONObject("entity")
                .getString("id");
    }

    /**
     * Extract debited amount (in paisa → convert to rupees).
     * Razorpay payload path: payload.payment.entity.amount (in paisa)
     */
    private double extractAmount(JSONObject root) {
        long amountPaisa = root.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity")
                .getLong("amount");
        return amountPaisa / 100.0;
    }

    /**
     * Extract Razorpay payment ID.
     * Razorpay payload path: payload.payment.entity.id
     */
    private String extractPaymentId(JSONObject root) {
        return root.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity")
                .getString("id");
    }
}

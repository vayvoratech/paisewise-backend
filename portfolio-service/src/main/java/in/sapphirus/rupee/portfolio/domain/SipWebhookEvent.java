package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit log for all Razorpay webhook events received by this service.
 * The {@code eventId} column has a UNIQUE constraint so that replayed webhooks
 * (Razorpay retries on 5xx) are idempotently rejected at the DB level before
 * any business logic runs.
 */
@Entity
@Table(name = "sip_webhook_events")
public class SipWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Razorpay's unique event ID (from the JSON payload {@code event.id}).
     * Used for idempotency — already processed events are skipped.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    /** e.g. mandate.approved, payment.captured, subscription.cancelled */
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    /** SIP this event was resolved to (may be null if resolution failed). */
    @Column(name = "sip_id")
    private UUID sipId;

    /** Full raw webhook JSON payload for debugging / replay. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** OK | SKIPPED | ERROR */
    @Column(nullable = false, length = 20)
    private String status = "OK";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    protected SipWebhookEvent() {}

    public SipWebhookEvent(String eventId, String eventType, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getSipId() { return sipId; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getProcessedAt() { return processedAt; }

    public void setSipId(UUID sipId) { this.sipId = sipId; }
    public void setStatus(String status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

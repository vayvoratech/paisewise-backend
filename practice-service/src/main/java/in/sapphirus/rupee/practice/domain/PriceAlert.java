package in.sapphirus.rupee.practice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_alerts")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(name = "condition", nullable = false, length = 10)
    private String condition; // GT, GTE, LT, LTE

    @Column(name = "target_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "ltp_at_creation", nullable = false, precision = 12, scale = 2)
    private BigDecimal ltpAtCreation;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, TRIGGERED, CANCELLED, EXPIRED

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "triggered_price", precision = 12, scale = 2)
    private BigDecimal triggeredPrice;

    @Column(name = "notification_sent", nullable = false)
    private boolean notificationSent = false;

    @Column(length = 200)
    private String note;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public PriceAlert() {}

    public PriceAlert(UUID userId, String symbol, String condition, BigDecimal targetPrice, BigDecimal ltpAtCreation, String note, Instant expiresAt) {
        this.userId = userId;
        this.symbol = symbol;
        this.condition = condition;
        this.targetPrice = targetPrice;
        this.ltpAtCreation = ltpAtCreation;
        this.note = note;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }

    public BigDecimal getLtpAtCreation() { return ltpAtCreation; }
    public void setLtpAtCreation(BigDecimal ltpAtCreation) { this.ltpAtCreation = ltpAtCreation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(Instant triggeredAt) { this.triggeredAt = triggeredAt; }

    public BigDecimal getTriggeredPrice() { return triggeredPrice; }
    public void setTriggeredPrice(BigDecimal triggeredPrice) { this.triggeredPrice = triggeredPrice; }

    public boolean isNotificationSent() { return notificationSent; }
    public void setNotificationSent(boolean notificationSent) { this.notificationSent = notificationSent; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

package in.sapphirus.rupee.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log", schema = "auth")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String action; // LOGIN, LOGOUT, OTP_SENT, OTP_VERIFIED, MPIN_SET, MPIN_CHANGED, ACCOUNT_LOCKED

    @Column(nullable = false)
    private String result; // SUCCESS, FAILURE

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AuditLog() {}

    public AuditLog(UUID userId, String action, String result, String failureReason, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.result = result;
        this.failureReason = failureReason;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

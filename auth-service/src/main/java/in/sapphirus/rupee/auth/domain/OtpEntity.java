package in.sapphirus.rupee.auth.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_verifications")
public class OtpEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone", nullable = false, length = 15)
    private String email; // Keep name email in Java, map to phone column in SQL

    @Column(name = "otp_hash", nullable = false, length = 60)
    private String otp; // Keep name otp in Java, map to otp_hash column in SQL

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private boolean verified = false; // Map verified in Java to is_used column in SQL

    @Column(name = "purpose", length = 30)
    private String purpose = "LOGIN";

    @Column(name = "attempts")
    private int attempts = 0;

    @Column(name = "max_attempts")
    private int maxAttempts = 3;

    @Column(name = "is_locked")
    private boolean isLocked = false;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_id", length = 200)
    private String deviceId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public OtpEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}

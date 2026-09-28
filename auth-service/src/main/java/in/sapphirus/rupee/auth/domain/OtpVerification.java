package in.sapphirus.rupee.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_verifications")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "otp_hash", nullable = false, length = 60)
    private String otpHash;

    @Column(nullable = false, length = 30)
    private String purpose = "LOGIN";

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_id", length = 200)
    private String deviceId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OtpVerification() {}

    public OtpVerification(String phone, String otpHash, String purpose, Instant expiresAt) {
        this.phone = phone;
        this.otpHash = otpHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public String getOtpHash() { return otpHash; }
    public String getPurpose() { return purpose; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public boolean isUsed() { return isUsed; }
    public boolean isLocked() { return isLocked; }
    public Instant getLockedUntil() { return lockedUntil; }
    public String getIpAddress() { return ipAddress; }
    public String getDeviceId() { return deviceId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setUsed(boolean used) { isUsed = used; }
    public void setLocked(boolean locked) { isLocked = locked; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}

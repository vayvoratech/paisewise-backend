package in.sapphirus.rupee.practice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paper_accounts")
public class PaperAccount {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = new BigDecimal("100000.00");

    @Column(name = "reserved_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(name = "last_reset_at", nullable = false)
    private Instant lastResetAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PaperAccount() {
    }

    public PaperAccount(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    public void setReservedBalance(BigDecimal reservedBalance) {
        this.reservedBalance = reservedBalance;
    }

    public Instant getLastResetAt() {
        return lastResetAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setLastResetAt(Instant lastResetAt) {
        this.lastResetAt = lastResetAt;
    }
}
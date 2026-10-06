package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false)
    private double amount;

    @Column(name = "balance_after", nullable = false)
    private double balanceAfter;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "ref_type", length = 30)
    private String refType;

    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Ledger() {}

    public Ledger(UUID userId, String type, double amount, double balanceAfter, String description) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public String getRefType() { return refType; }
    public UUID getRefId() { return refId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setRefType(String refType) { this.refType = refType; }
    public void setRefId(UUID refId) { this.refId = refId; }
}

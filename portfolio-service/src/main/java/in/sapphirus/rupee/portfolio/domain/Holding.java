package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holdings")
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "avg_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal avgCost;

    @Column(name = "total_invested", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalInvested;

    @Column(nullable = false, length = 10)
    private String product = "CNC";

    @Column(name = "is_paper", nullable = false)
    private boolean isPaper = false;

    @Column(name = "first_bought_at", nullable = false)
    private Instant firstBoughtAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Holding() {}

    public Holding(UUID userId, String symbol, int quantity, BigDecimal avgCost,
                   BigDecimal totalInvested, boolean isPaper) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgCost = avgCost;
        this.totalInvested = totalInvested;
        this.isPaper = isPaper;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public BigDecimal getAvgCost() { return avgCost; }
    public BigDecimal getTotalInvested() { return totalInvested; }
    public String getProduct() { return product; }
    public boolean isPaper() { return isPaper; }
    public Instant getFirstBoughtAt() { return firstBoughtAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holdings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "symbol", "product", "is_paper"})
})
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String symbol;

    private String name;
    private String emoji;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "avg_cost")
    private double avgPrice;

    private double currentPrice;

    @Column(name = "total_invested")
    private double totalInvested;

    @Column(length = 10)
    private String product = "CNC";

    @Column(name = "is_paper")
    private boolean isPaper = true;

    @Column(name = "first_bought_at")
    private Instant firstBoughtAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(length = 500)
    private String note;

    public Holding() {}

    public Holding(UUID userId, String symbol, int quantity, double avgPrice, double currentPrice, String note) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.currentPrice = currentPrice;
        this.totalInvested = Math.round(quantity * avgPrice);
        this.note = note;
        this.createdAtTimestamp();
    }

    public Holding(UUID userId, String symbol, int quantity, BigDecimal avgCost, BigDecimal totalInvested, boolean isPaper) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgPrice = avgCost != null ? avgCost.doubleValue() : 0.0;
        this.totalInvested = totalInvested != null ? totalInvested.doubleValue() : (quantity * this.avgPrice);
        this.isPaper = isPaper;
        this.createdAtTimestamp();
    }

    private void createdAtTimestamp() {
        this.firstBoughtAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public int getQuantity() { return quantity; }
    public int getShares() { return quantity; }
    public double getAvgPrice() { return avgPrice; }
    public BigDecimal getAvgCost() { return BigDecimal.valueOf(avgPrice); }
    public double getCurrentPrice() { return currentPrice; }
    public double getTotalInvested() { return totalInvested; }
    public String getProduct() { return product; }
    public boolean isPaper() { return isPaper; }
    public Instant getFirstBoughtAt() { return firstBoughtAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getNote() { return note; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setShares(int shares) { this.quantity = shares; }
    public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setTotalInvested(double totalInvested) { this.totalInvested = totalInvested; }
    public void setProduct(String product) { this.product = product; }
    public void setPaper(boolean paper) { this.isPaper = paper; }
    public void setFirstBoughtAt(Instant firstBoughtAt) { this.firstBoughtAt = firstBoughtAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setNote(String note) { this.note = note; }
}


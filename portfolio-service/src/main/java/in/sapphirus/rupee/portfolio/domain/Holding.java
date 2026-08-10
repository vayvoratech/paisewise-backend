package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holdings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "symbol", "product", "is_paper"})
})
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    private String symbol;
    private String name;
    private String emoji;

    @Column(name = "quantity")
    private int shares;

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

    protected Holding() {}

    public Holding(String userId, String symbol, String name, String emoji, int shares,
                   double avgPrice, double currentPrice, String note) {
        this.userId = userId;
        this.symbol = symbol;
        this.name = name;
        this.emoji = emoji;
        this.shares = shares;
        this.avgPrice = avgPrice;
        this.currentPrice = currentPrice;
        this.totalInvested = Math.round(shares * avgPrice);
        this.note = note;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public int getShares() { return shares; }
    public double getAvgPrice() { return avgPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public double getTotalInvested() { return totalInvested; }
    public String getProduct() { return product; }
    public boolean isPaper() { return isPaper; }
    public Instant getFirstBoughtAt() { return firstBoughtAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getNote() { return note; }

    public void setShares(int shares) { this.shares = shares; }
    public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setTotalInvested(double totalInvested) { this.totalInvested = totalInvested; }
    public void setProduct(String product) { this.product = product; }
    public void setPaper(boolean paper) { this.isPaper = paper; }
    public void setFirstBoughtAt(Instant firstBoughtAt) { this.firstBoughtAt = firstBoughtAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setNote(String note) { this.note = note; }
}

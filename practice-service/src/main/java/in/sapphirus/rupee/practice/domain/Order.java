package in.sapphirus.rupee.practice.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** A paper-trading order. Practice money only — no real funds. */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    private String symbol;
    private String side; // BUY | SELL
    private int shares;
    private double pricePerShare;
    private double totalAmount;
    private String orderType; // MARKET | LIMIT | STOP_LOSS
    private int xpEarned;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Order() {}

    public Order(String userId, String symbol, String side, int shares, double pricePerShare,
                 String orderType, int xpEarned) {
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.shares = shares;
        this.pricePerShare = pricePerShare;
        this.totalAmount = Math.round(shares * pricePerShare);
        this.orderType = orderType;
        this.xpEarned = xpEarned;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getSide() { return side; }
    public int getShares() { return shares; }
    public double getPricePerShare() { return pricePerShare; }
    public double getTotalAmount() { return totalAmount; }
    public String getOrderType() { return orderType; }
    public int getXpEarned() { return xpEarned; }
    public Instant getCreatedAt() { return createdAt; }
}

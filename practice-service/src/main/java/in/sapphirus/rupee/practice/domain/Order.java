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

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "client_order_id", unique = true, length = 64)
    private String clientOrderId;

    private String symbol;
    
    @Column(length = 5)
    private String exchange = "NSE";

    private String side; // BUY | SELL

    @Column(name = "quantity")
    private int shares;

    @Column(name = "filled_qty")
    private int filledQty = 0;

    @Column(name = "price")
    private double pricePerShare;

    @Column(name = "trigger_price")
    private Double triggerPrice;

    @Column(name = "avg_price")
    private Double avgPrice;

    private double totalAmount;

    @Column(name = "order_type")
    private String orderType; // MARKET | LIMIT | STOP_LOSS

    @Column(length = 20)
    private String status = "COMPLETED";

    @Column(name = "broker_order_id", length = 50)
    private String brokerOrderId;

    @Column(name = "broker_message", columnDefinition = "TEXT")
    private String brokerMessage;

    @Column(name = "is_paper")
    private boolean isPaper = true;

    @Column(length = 5)
    private String validity = "DAY";

    private int xpEarned;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

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
        this.clientOrderId = UUID.randomUUID().toString();
        this.filledQty = shares;
        this.avgPrice = pricePerShare;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getClientOrderId() { return clientOrderId; }
    public String getSymbol() { return symbol; }
    public String getExchange() { return exchange; }
    public String getSide() { return side; }
    public int getShares() { return shares; }
    public int getFilledQty() { return filledQty; }
    public double getPricePerShare() { return pricePerShare; }
    public Double getTriggerPrice() { return triggerPrice; }
    public Double getAvgPrice() { return avgPrice; }
    public double getTotalAmount() { return totalAmount; }
    public String getOrderType() { return orderType; }
    public String getStatus() { return status; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public String getBrokerMessage() { return brokerMessage; }
    public boolean isPaper() { return isPaper; }
    public String getValidity() { return validity; }
    public int getXpEarned() { return xpEarned; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public void setFilledQty(int qty) { this.filledQty = qty; }
    public void setTriggerPrice(Double triggerPrice) { this.triggerPrice = triggerPrice; }
    public void setAvgPrice(Double avgPrice) { this.avgPrice = avgPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setBrokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; }
    public void setBrokerMessage(String msg) { this.brokerMessage = msg; }
    public void setPaper(boolean paper) { this.isPaper = paper; }
    public void setValidity(String validity) { this.validity = validity; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

package in.sapphirus.rupee.practice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A trading order — real or paper (see isPaper). Matches practice.orders (V2 schema). */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_order_id", nullable = false, unique = true, length = 64)
    private String clientOrderId;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false, length = 5)
    private String exchange = "NSE"; // NSE | BSE

    @Column(name = "side", nullable = false, length = 10)
    private String side; // BUY | SELL

    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType; // MARKET | LIMIT | SL | SL-M

    @Column(name = "product", length = 10)
    private String product = "CNC"; // CNC | MIS | NRML

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "filled_qty", nullable = false)
    private int filledQty = 0;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "trigger_price", precision = 12, scale = 2)
    private BigDecimal triggerPrice;

    @Column(name = "avg_price", precision = 12, scale = 4)
    private BigDecimal avgPrice;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "broker_order_id", length = 50)
    private String brokerOrderId;

    @Column(name = "broker_message", columnDefinition = "TEXT")
    private String brokerMessage;

    @Column(name = "is_paper", nullable = false)
    private boolean isPaper = true;

    @Column(nullable = false, length = 5)
    private String validity = "DAY"; // DAY | IOC

    @Column(name = "xp_earned")
    private int xpEarned = 0;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Order() {}

    public Order(UUID userId, String clientOrderId, String symbol, String exchange, String side,
                 String orderType, String product, int quantity, boolean isPaper) {
        this.userId = userId;
        this.clientOrderId = clientOrderId != null ? clientOrderId : UUID.randomUUID().toString();
        this.symbol = symbol;
        this.exchange = exchange != null ? exchange : "NSE";
        this.side = side;
        this.orderType = orderType;
        this.product = product != null ? product : "CNC";
        this.quantity = quantity;
        this.isPaper = isPaper;
        this.placedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Order(String userIdStr, String symbol, String side, int shares, double pricePerShare, String orderType, int xpEarned) {
        this.userId = UUID.fromString(userIdStr);
        this.clientOrderId = UUID.randomUUID().toString();
        this.symbol = symbol;
        this.side = side;
        this.quantity = shares;
        this.filledQty = shares;
        this.price = BigDecimal.valueOf(pricePerShare);
        this.avgPrice = BigDecimal.valueOf(pricePerShare);
        this.orderType = orderType;
        this.status = "COMPLETED";
        this.isPaper = true;
        this.xpEarned = xpEarned;
        this.placedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getClientOrderId() { return clientOrderId; }
    public String getSymbol() { return symbol; }
    public String getExchange() { return exchange; }
    public String getSide() { return side; }
    public String getOrderType() { return orderType; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public int getShares() { return quantity; }
    public int getFilledQty() { return filledQty; }
    public BigDecimal getPrice() { return price; }
    public double getPricePerShare() { return price != null ? price.doubleValue() : 0.0; }
    public BigDecimal getTriggerPrice() { return triggerPrice; }
    public BigDecimal getAvgPrice() { return avgPrice; }
    public String getStatus() { return status; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public String getBrokerMessage() { return brokerMessage; }
    public boolean isPaper() { return isPaper; }
    public String getValidity() { return validity; }
    public int getXpEarned() { return xpEarned; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getCreatedAt() { return placedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public void setStatus(String status) { this.status = status; }
    public void setFilledQty(int filledQty) { this.filledQty = filledQty; }
    public void setAvgPrice(BigDecimal avgPrice) { this.avgPrice = avgPrice; }
    public void setBrokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; }
    public void setBrokerMessage(String brokerMessage) { this.brokerMessage = brokerMessage; }
    public void setPaper(boolean paper) { this.isPaper = paper; }
    public void setValidity(String validity) { this.validity = validity; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}


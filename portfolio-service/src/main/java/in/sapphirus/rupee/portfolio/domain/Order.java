package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Real trading order entity for broker integration (Fyers).
 * Matches portfolio.orders.
 */
@Entity
@Table(name = "orders", schema = "portfolio")
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

    @Column(nullable = false, length = 10)
    private String exchange = "NSE";

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
    private String status = "PENDING"; // PENDING | OPEN | PARTIAL | COMPLETE | REJECTED | CANCELLED

    @Column(name = "broker_order_id", length = 50)
    private String brokerOrderId;

    @Column(name = "broker_message", columnDefinition = "TEXT")
    private String brokerMessage;

    @Column(name = "is_paper", nullable = false)
    private boolean isPaper = false;

    @Column(nullable = false, length = 5)
    private String validity = "DAY"; // DAY | IOC

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Order() {}

    public Order(UUID userId, String clientOrderId, String symbol, String exchange, String side,
                 String orderType, String product, int quantity, BigDecimal price, BigDecimal triggerPrice,
                 String validity, boolean isPaper) {
        this.userId = userId;
        this.clientOrderId = clientOrderId != null && !clientOrderId.isBlank()
                ? clientOrderId
                : UUID.randomUUID().toString();
        this.symbol = symbol;
        this.exchange = exchange != null ? exchange : "NSE";
        this.side = side != null ? side.toUpperCase() : "BUY";
        this.orderType = orderType != null ? orderType.toUpperCase() : "MARKET";
        this.product = product != null ? product.toUpperCase() : "CNC";
        this.quantity = quantity;
        this.price = price;
        this.triggerPrice = triggerPrice;
        this.validity = validity != null ? validity.toUpperCase() : "DAY";
        this.isPaper = isPaper;
        this.status = "PENDING";
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
    public int getFilledQty() { return filledQty; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getTriggerPrice() { return triggerPrice; }
    public BigDecimal getAvgPrice() { return avgPrice; }
    public String getStatus() { return status; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public String getBrokerMessage() { return brokerMessage; }
    public boolean isPaper() { return isPaper; }
    public String getValidity() { return validity; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public void setSide(String side) { this.side = side; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public void setProduct(String product) { this.product = product; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setFilledQty(int filledQty) { this.filledQty = filledQty; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setTriggerPrice(BigDecimal triggerPrice) { this.triggerPrice = triggerPrice; }
    public void setAvgPrice(BigDecimal avgPrice) { this.avgPrice = avgPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setBrokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; }
    public void setBrokerMessage(String brokerMessage) { this.brokerMessage = brokerMessage; }
    public void setPaper(boolean paper) { isPaper = paper; }
    public void setValidity(String validity) { this.validity = validity; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

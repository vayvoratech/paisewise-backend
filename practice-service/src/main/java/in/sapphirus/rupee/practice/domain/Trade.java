package in.sapphirus.rupee.practice.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false, length = 5)
    private String exchange;

    @Column(name = "fill_qty", nullable = false)
    private int fillQty;

    @Column(name = "fill_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal fillPrice;

    @Column(name = "net_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "broker_trade_id", length = 50)
    private String brokerTradeId;

    @Column(name = "is_paper", nullable = false)
    private boolean isPaper = false;

    @Column(name = "traded_at", nullable = false)
    private Instant tradedAt = Instant.now();

    protected Trade() {
    }

    public Trade(
            UUID orderId,
            UUID userId,
            String symbol,
            String exchange,
            int fillQty,
            BigDecimal fillPrice,
            BigDecimal netAmount,
            boolean isPaper
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.exchange = exchange;
        this.fillQty = fillQty;
        this.fillPrice = fillPrice;
        this.netAmount = netAmount;
        this.isPaper = isPaper;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getExchange() {
        return exchange;
    }

    public int getFillQty() {
        return fillQty;
    }

    public BigDecimal getFillPrice() {
        return fillPrice;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public String getBrokerTradeId() {
        return brokerTradeId;
    }

    public boolean isPaper() {
        return isPaper;
    }

    public Instant getTradedAt() {
        return tradedAt;
    }
}
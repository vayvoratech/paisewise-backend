package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
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

    @Column(nullable = false, length = 10)
    private String side;

    @Column(name = "fill_qty", nullable = false)
    private int fillQty;

    @Column(name = "fill_price", nullable = false)
    private double fillPrice;

    @Column(nullable = false)
    private double brokerage = 0.0;

    @Column(nullable = false)
    private double stt = 0.0;

    @Column(nullable = false)
    private double gst = 0.0;

    @Column(name = "sebi_charges", nullable = false)
    private double sebiCharges = 0.0;

    @Column(name = "stamp_duty", nullable = false)
    private double stampDuty = 0.0;

    @Column(name = "total_charges", nullable = false)
    private double totalCharges = 0.0;

    @Column(name = "net_amount", nullable = false)
    private double netAmount;

    @Column(name = "broker_trade_id", unique = true, length = 50)
    private String brokerTradeId;

    @Column(name = "is_paper", nullable = false)
    private boolean isPaper = true;

    @Column(name = "traded_at", nullable = false)
    private Instant tradedAt = Instant.now();

    protected Trade() {}

    public Trade(UUID orderId, UUID userId, String symbol, String exchange, String side, int fillQty, double fillPrice) {
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.exchange = exchange;
        this.side = side;
        this.fillQty = fillQty;
        this.fillPrice = fillPrice;
        this.netAmount = Math.round(fillQty * fillPrice);
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getExchange() { return exchange; }
    public String getSide() { return side; }
    public int getFillQty() { return fillQty; }
    public double getFillPrice() { return fillPrice; }
    public double getBrokerage() { return brokerage; }
    public double getStt() { return stt; }
    public double getGst() { return gst; }
    public double getSebiCharges() { return sebiCharges; }
    public double getStampDuty() { return stampDuty; }
    public double getTotalCharges() { return totalCharges; }
    public double getNetAmount() { return netAmount; }
    public String getBrokerTradeId() { return brokerTradeId; }
    public boolean isPaper() { return isPaper; }
    public Instant getTradedAt() { return tradedAt; }

    public void setBrokerage(double brokerage) { this.brokerage = brokerage; }
    public void setStt(double stt) { this.stt = stt; }
    public void setGst(double gst) { this.gst = gst; }
    public void setSebiCharges(double sebiCharges) { this.sebiCharges = sebiCharges; }
    public void setStampDuty(double stampDuty) { this.stampDuty = stampDuty; }
    public void setTotalCharges(double totalCharges) { this.totalCharges = totalCharges; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }
    public void setBrokerTradeId(String brokerTradeId) { this.brokerTradeId = brokerTradeId; }
    public void setPaper(boolean paper) { this.isPaper = paper; }
    public void setTradedAt(Instant tradedAt) { this.tradedAt = tradedAt; }
}

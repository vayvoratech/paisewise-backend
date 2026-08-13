package in.sapphirus.rupee.market.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticks")
@IdClass(TickId.class)
public class Tick {

    @Id
    @Column(nullable = false)
    private Instant time;

    @Id
    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false)
    private Double ltp;

    private Double open;
    private Double high;
    private Double low;
    private Double close;

    @Column(name = "prev_close")
    private Double prevClose;

    private Long volume;

    @Column(name = "avg_price")
    private Double avgPrice;

    @Column(name = "upper_circuit")
    private Double upperCircuit;

    @Column(name = "lower_circuit")
    private Double lowerCircuit;

    private Long oi;

    @Column(name = "oi_day_high")
    private Long oiDayHigh;

    @Column(name = "oi_day_low")
    private Long oiDayLow;

    @Column(name = "bid_price")
    private Double bidPrice;

    @Column(name = "ask_price")
    private Double askPrice;

    @Column(name = "bid_qty")
    private Integer bidQty;

    @Column(name = "ask_qty")
    private Integer askQty;

    @Column(name = "change_abs")
    private Double changeAbs;

    @Column(name = "change_pct")
    private Double changePct;

    protected Tick() {}

    public Tick(Instant time, String symbol, Double ltp) {
        this.time = time;
        this.symbol = symbol;
        this.ltp = ltp;
    }

    public Instant getTime() { return time; }
    public String getSymbol() { return symbol; }
    public Double getLtp() { return ltp; }
    public Double getOpen() { return open; }
    public Double getHigh() { return high; }
    public Double getLow() { return low; }
    public Double getClose() { return close; }
    public Double getPrevClose() { return prevClose; }
    public Long getVolume() { return volume; }
    public Double getAvgPrice() { return avgPrice; }
    public Double getUpperCircuit() { return upperCircuit; }
    public Double getLowerCircuit() { return lowerCircuit; }
    public Long getOi() { return oi; }
    public Long getOiDayHigh() { return oiDayHigh; }
    public Long getOiDayLow() { return oiDayLow; }
    public Double getBidPrice() { return bidPrice; }
    public Double getAskPrice() { return askPrice; }
    public Integer getBidQty() { return bidQty; }
    public Integer getAskQty() { return askQty; }
    public Double getChangeAbs() { return changeAbs; }
    public Double getChangePct() { return changePct; }

    public void setTime(Instant time) { this.time = time; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setLtp(Double ltp) { this.ltp = ltp; }
    public void setOpen(Double open) { this.open = open; }
    public void setHigh(Double high) { this.high = high; }
    public void setLow(Double low) { this.low = low; }
    public void setClose(Double close) { this.close = close; }
    public void setVolume(Long volume) { this.volume = volume; }
}

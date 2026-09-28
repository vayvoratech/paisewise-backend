package in.sapphirus.rupee.market.dto;

import java.time.Instant;

public class CandleDto {
    private Instant bucket;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;

    public CandleDto() {}

    public CandleDto(Instant bucket, Double open, Double high, Double low, Double close, Long volume) {
        this.bucket = bucket;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public Instant getBucket() { return bucket; }
    public void setBucket(Instant bucket) { this.bucket = bucket; }

    public Double getOpen() { return open; }
    public void setOpen(Double open) { this.open = open; }

    public Double getHigh() { return high; }
    public void setHigh(Double high) { this.high = high; }

    public Double getLow() { return low; }
    public void setLow(Double low) { this.low = low; }

    public Double getClose() { return close; }
    public void setClose(Double close) { this.close = close; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
}

package in.sapphirus.rupee.market.dto;

import java.io.Serializable;
import java.util.List;

public class StockQuoteDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private String name;
    private Double price;
    private Double changePct;
    private List<Double> trend;
    private String emoji;

    public StockQuoteDto() {}

    public StockQuoteDto(String symbol, String name, Double price, Double changePct, List<Double> trend, String emoji) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.changePct = changePct;
        this.trend = trend;
        this.emoji = emoji;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getChangePct() { return changePct; }
    public void setChangePct(Double changePct) { this.changePct = changePct; }

    public List<Double> getTrend() { return trend; }
    public void setTrend(List<Double> trend) { this.trend = trend; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
}

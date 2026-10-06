package in.sapphirus.rupee.practice.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    private String symbol;

    private String name;
    private double price;
    private double changePct;
    private String emoji;

    /** JSON array of recent prices for the sparkline. */
    @Column(length = 1000)
    private String trendJson;

    protected Stock() {}

    public Stock(String symbol, String name, double price, double changePct, String emoji, String trendJson) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.changePct = changePct;
        this.emoji = emoji;
        this.trendJson = trendJson;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public double getChangePct() { return changePct; }
    public String getEmoji() { return emoji; }
    public String getTrendJson() { return trendJson; }
}

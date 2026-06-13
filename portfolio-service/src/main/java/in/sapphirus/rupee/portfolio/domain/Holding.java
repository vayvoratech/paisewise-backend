package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "symbol"}))
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    private String symbol;
    private String name;
    private String emoji;
    private int shares;
    private double avgPrice;
    private double currentPrice;

    @Column(length = 500)
    private String note;

    protected Holding() {}

    public Holding(String userId, String symbol, String name, String emoji, int shares,
                   double avgPrice, double currentPrice, String note) {
        this.userId = userId;
        this.symbol = symbol;
        this.name = name;
        this.emoji = emoji;
        this.shares = shares;
        this.avgPrice = avgPrice;
        this.currentPrice = currentPrice;
        this.note = note;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public int getShares() { return shares; }
    public double getAvgPrice() { return avgPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public String getNote() { return note; }
}

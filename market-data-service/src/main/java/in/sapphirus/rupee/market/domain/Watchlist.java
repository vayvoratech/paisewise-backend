package in.sapphirus.rupee.market.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watchlists", schema = "public")
public class Watchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    public Watchlist() {}

    public Watchlist(UUID userId, String symbol, Integer sortOrder) {
        this.userId = userId;
        this.symbol = symbol;
        this.sortOrder = sortOrder;
        this.addedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}

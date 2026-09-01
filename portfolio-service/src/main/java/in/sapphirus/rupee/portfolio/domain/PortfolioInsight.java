package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "portfolio_insights")
public class PortfolioInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "insight_date", nullable = false)
    private LocalDate insightDate;

    @Column(nullable = false, length = 5)
    private String language = "en";

    @Column(name = "insight_text", nullable = false, columnDefinition = "TEXT")
    private String insightText;

    @Column(name = "portfolio_value")
    private Double portfolioValue;

    @Column(name = "daily_change_pct")
    private Double dailyChangePct;

    @Column(name = "top_gainer_symbol", length = 30)
    private String topGainerSymbol;

    @Column(name = "top_loser_symbol", length = 30)
    private String topLoserSymbol;

    @Column(name = "market_summary", columnDefinition = "TEXT")
    private String marketSummary;

    @Column(name = "generation_status", length = 20)
    private String generationStatus = "SUCCESS";

    @Column(name = "llm_model_used", length = 50)
    private String llmModelUsed;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PortfolioInsight() {}

    public PortfolioInsight(UUID userId, LocalDate insightDate, String insightText) {
        this.userId = userId;
        this.insightDate = insightDate;
        this.insightText = insightText;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getInsightDate() { return insightDate; }
    public String getLanguage() { return language; }
    public String getInsightText() { return insightText; }
    public Double getPortfolioValue() { return portfolioValue; }
    public Double getDailyChangePct() { return dailyChangePct; }
    public String getTopGainerSymbol() { return topGainerSymbol; }
    public String getTopLoserSymbol() { return topLoserSymbol; }
    public String getMarketSummary() { return marketSummary; }
    public String getGenerationStatus() { return generationStatus; }
    public String getLlmModelUsed() { return llmModelUsed; }
    public Integer getTokensUsed() { return tokensUsed; }
    public Long getGenerationTimeMs() { return generationTimeMs; }
    public Instant getCreatedAt() { return createdAt; }

    public void setLanguage(String language) { this.language = language; }
    public void setPortfolioValue(Double val) { this.portfolioValue = val; }
    public void setDailyChangePct(Double val) { this.dailyChangePct = val; }
    public void setTopGainerSymbol(String symbol) { this.topGainerSymbol = symbol; }
    public void setTopLoserSymbol(String symbol) { this.topLoserSymbol = symbol; }
    public void setMarketSummary(String summary) { this.marketSummary = summary; }
    public void setGenerationStatus(String status) { this.generationStatus = status; }
    public void setLlmModelUsed(String model) { this.llmModelUsed = model; }
    public void setTokensUsed(Integer tokens) { this.tokensUsed = tokens; }
    public void setGenerationTimeMs(Long timeMs) { this.generationTimeMs = timeMs; }
}

package in.sapphirus.rupee.market.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "symbols")
public class Symbol {

    @Id
    @Column(length = 30)
    private String symbol;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(nullable = false, length = 5)
    private String exchange;

    @Column(name = "instrument_type", nullable = false, length = 20)
    private String instrumentType;

    @Column(length = 12)
    private String isin;

    @Column(length = 5)
    private String series;

    @Column(length = 100)
    private String sector;

    @Column(length = 100)
    private String industry;

    @Column(name = "market_cap_category", length = 10)
    private String marketCapCategory;

    @Column(name = "face_value")
    private Double faceValue;

    @Column(name = "lot_size", nullable = false)
    private Integer lotSize = 1;

    @Column(name = "tick_size", nullable = false)
    private Double tickSize = 0.05;

    @Column(name = "is_fo_enabled", nullable = false)
    private Boolean isFoEnabled = false;

    @Column(name = "is_slb_enabled", nullable = false)
    private Boolean isSlbEnabled = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_suspended", nullable = false)
    private Boolean isSuspended = false;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(name = "upper_circuit_pct")
    private Double upperCircuitPct;

    @Column(name = "lower_circuit_pct")
    private Double lowerCircuitPct;

    @Column(name = "bse_code", length = 10)
    private String bseCode;

    @Column(name = "nse_token")
    private Integer nseToken;

    @Column(name = "fyers_symbol", length = 30)
    private String fyersSymbol;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "website_url", columnDefinition = "TEXT")
    private String websiteUrl;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "demat_lot_size")
    private Integer dematLotSize;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Symbol() {}

    public Symbol(String symbol, String companyName, String shortName, String exchange, String instrumentType) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.shortName = shortName;
        this.exchange = exchange;
        this.instrumentType = instrumentType;
    }

    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public String getShortName() { return shortName; }
    public String getExchange() { return exchange; }
    public String getInstrumentType() { return instrumentType; }
    public String getIsin() { return isin; }
    public String getSeries() { return series; }
    public String getSector() { return sector; }
    public String getIndustry() { return industry; }
    public String getMarketCapCategory() { return marketCapCategory; }
    public Double getFaceValue() { return faceValue; }
    public Integer getLotSize() { return lotSize; }
    public Double getTickSize() { return tickSize; }
    public Boolean getIsFoEnabled() { return isFoEnabled; }
    public Boolean getIsSlbEnabled() { return isSlbEnabled; }
    public Boolean getIsActive() { return isActive; }
    public Boolean getIsSuspended() { return isSuspended; }
    public String getSuspensionReason() { return suspensionReason; }
    public Double getUpperCircuitPct() { return upperCircuitPct; }
    public Double getLowerCircuitPct() { return lowerCircuitPct; }
    public String getBseCode() { return bseCode; }
    public Integer getNseToken() { return nseToken; }
    public String getFyersSymbol() { return fyersSymbol; }
    public String getLogoUrl() { return logoUrl; }
    public String getDescription() { return description; }
    public String getWebsiteUrl() { return websiteUrl; }
    public LocalDate getListingDate() { return listingDate; }
    public Integer getDematLotSize() { return dematLotSize; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

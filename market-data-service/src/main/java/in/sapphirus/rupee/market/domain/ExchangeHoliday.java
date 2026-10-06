package in.sapphirus.rupee.market.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "exchange_holidays", schema = "public")
public class ExchangeHoliday {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 5)
    private String exchange;

    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    @Column(name = "holiday_type", nullable = false, length = 30)
    private String holidayType;

    @Column(name = "is_trading_day", nullable = false)
    private Boolean isTradingDay = false;

    @Column(name = "trading_start")
    private LocalTime tradingStart;

    @Column(name = "trading_end")
    private LocalTime tradingEnd;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ExchangeHoliday() {}

    public ExchangeHoliday(LocalDate holidayDate, String exchange, String holidayName, String holidayType, Boolean isTradingDay, LocalTime tradingStart, LocalTime tradingEnd, String description) {
        this.holidayDate = holidayDate;
        this.exchange = exchange;
        this.holidayName = holidayName;
        this.holidayType = holidayType;
        this.isTradingDay = isTradingDay;
        this.tradingStart = tradingStart;
        this.tradingEnd = tradingEnd;
        this.description = description;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }
    public String getHolidayType() { return holidayType; }
    public void setHolidayType(String holidayType) { this.holidayType = holidayType; }
    public Boolean getIsTradingDay() { return isTradingDay; }
    public void setIsTradingDay(Boolean isTradingDay) { this.isTradingDay = isTradingDay; }
    public LocalTime getTradingStart() { return tradingStart; }
    public void setTradingStart(LocalTime tradingStart) { this.tradingStart = tradingStart; }
    public LocalTime getTradingEnd() { return tradingEnd; }
    public void setTradingEnd(LocalTime tradingEnd) { this.tradingEnd = tradingEnd; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

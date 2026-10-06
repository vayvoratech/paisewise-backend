package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "mf_schemes")
public class MfScheme {

    @Id
    @Column(name = "scheme_code", length = 20)
    private String schemeCode;

    @Column(length = 20)
    private String isin;

    @Column(name = "scheme_name", nullable = false, length = 200)
    private String schemeName;

    @Column(name = "amc_name", nullable = false, length = 100)
    private String amcName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(name = "scheme_type", length = 30)
    private String schemeType;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    private double nav;

    @Column(name = "nav_date")
    private LocalDate navDate;

    @Column(name = "min_sip_amount")
    private double minSipAmount = 500.0;

    @Column(name = "min_lumpsum")
    private double minLumpsum = 1000.0;

    @Column(name = "returns_1y")
    private Double returns1y;

    @Column(name = "returns_3y")
    private Double returns3y;

    @Column(name = "returns_5y")
    private Double returns5y;

    @Column(name = "expense_ratio")
    private Double expenseRatio;

    @Column(name = "fund_manager", length = 100)
    private String fundManager;

    @Column(name = "fund_size_cr")
    private Double fundSizeCr;

    @Column(name = "launch_date")
    private LocalDate launchDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected MfScheme() {}

    public MfScheme(String schemeCode, String schemeName, String amcName, String category, double nav) {
        this.schemeCode = schemeCode;
        this.schemeName = schemeName;
        this.amcName = amcName;
        this.category = category;
        this.nav = nav;
    }

    public String getSchemeCode() { return schemeCode; }
    public String getIsin() { return isin; }
    public String getSchemeName() { return schemeName; }
    public String getAmcName() { return amcName; }
    public String getCategory() { return category; }
    public String getSubCategory() { return subCategory; }
    public String getSchemeType() { return schemeType; }
    public String getRiskLevel() { return riskLevel; }
    public double getNav() { return nav; }
    public LocalDate getNavDate() { return navDate; }
    public double getMinSipAmount() { return minSipAmount; }
    public double getMinLumpsum() { return minLumpsum; }
    public Double getReturns1y() { return returns1y; }
    public Double getReturns3y() { return returns3y; }
    public Double getReturns5y() { return returns5y; }
    public Double getExpenseRatio() { return expenseRatio; }
    public String getFundManager() { return fundManager; }
    public Double getFundSizeCr() { return fundSizeCr; }
    public LocalDate getLaunchDate() { return launchDate; }
    public boolean isActive() { return isActive; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setIsin(String isin) { this.isin = isin; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public void setSchemeType(String schemeType) { this.schemeType = schemeType; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setNav(double nav) { this.nav = nav; }
    public void setNavDate(LocalDate date) { this.navDate = date; }
    public void setMinSipAmount(double amt) { this.minSipAmount = amt; }
    public void setMinLumpsum(double amt) { this.minLumpsum = amt; }
    public void setReturns1y(Double ret) { this.returns1y = ret; }
    public void setReturns3y(Double ret) { this.returns3y = ret; }
    public void setReturns5y(Double ret) { this.returns5y = ret; }
    public void setExpenseRatio(Double er) { this.expenseRatio = er; }
    public void setFundManager(String manager) { this.fundManager = manager; }
    public void setFundSizeCr(Double size) { this.fundSizeCr = size; }
    public void setLaunchDate(LocalDate date) { this.launchDate = date; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setUpdatedAt(Instant val) { this.updatedAt = val; }
}

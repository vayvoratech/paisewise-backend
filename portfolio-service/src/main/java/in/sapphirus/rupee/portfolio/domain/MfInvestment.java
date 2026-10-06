package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mf_investments")
public class MfInvestment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "scheme_code", nullable = false, length = 20)
    private String schemeCode;

    @Column(name = "sip_id")
    private UUID sipId;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType = "LUMPSUM"; // LUMPSUM | SIP | REDEMPTION

    @Column(nullable = false, length = 30)
    private String status = "PENDING"; // PENDING | COMPLETED | FAILED

    @Column(nullable = false)
    private double amount;

    @Column(name = "nav_applied")
    private Double navApplied;

    @Column(name = "units_allotted")
    private Double unitsAllotted;

    @Column(name = "folio_number", length = 50)
    private String folioNumber;

    @Column(name = "bse_order_id", length = 50)
    private String bseOrderId;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate = Instant.now();

    @Column(name = "allotment_date")
    private Instant allotmentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected MfInvestment() {}

    public MfInvestment(UUID userId, String schemeCode, double amount) {
        this.userId = userId;
        this.schemeCode = schemeCode;
        this.amount = amount;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getSchemeCode() { return schemeCode; }
    public UUID getSipId() { return sipId; }
    public String getTransactionType() { return transactionType; }
    public String getStatus() { return status; }
    public double getAmount() { return amount; }
    public Double getNavApplied() { return navApplied; }
    public Double getUnitsAllotted() { return unitsAllotted; }
    public String getFolioNumber() { return folioNumber; }
    public String getBseOrderId() { return bseOrderId; }
    public Instant getTransactionDate() { return transactionDate; }
    public Instant getAllotmentDate() { return allotmentDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setSipId(UUID sipId) { this.sipId = sipId; }
    public void setTransactionType(String type) { this.transactionType = type; }
    public void setStatus(String status) { this.status = status; }
    public void setNavApplied(Double nav) { this.navApplied = nav; }
    public void setUnitsAllotted(Double units) { this.unitsAllotted = units; }
    public void setFolioNumber(String folio) { this.folioNumber = folio; }
    public void setBseOrderId(String orderId) { this.bseOrderId = orderId; }
    public void setAllotmentDate(Instant date) { this.allotmentDate = date; }
    public void setUpdatedAt(Instant val) { this.updatedAt = val; }
}

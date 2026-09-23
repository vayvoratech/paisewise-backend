package in.sapphirus.rupee.portfolio.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "sips")
public class Sip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "scheme_code", nullable = false, length = 20)
    private String schemeCode;

    @Column(name = "goal_id")
    private UUID goalId;

    @Column(nullable = false, length = 20)
    private String frequency = "MONTHLY"; // WEEKLY | MONTHLY | QUARTERLY

    @Column(nullable = false)
    private double amount;

    @Column(name = "debit_day", nullable = false)
    private int debitDay = 5;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE | PAUSED | COMPLETED | CANCELLED

    @Column(name = "upi_mandate_id", length = 100)
    private String upiMandateId;

    /**
     * UPI mandate lifecycle status returned by Razorpay:
     * CREATED | APPROVED | REJECTED | PAUSED | CANCELLED | COMPLETED
     */
    @Column(name = "upi_mandate_status", length = 30)
    private String upiMandateStatus;

    @Column(name = "razorpay_subscription_id", length = 100)
    private String razorpaySubscriptionId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate = LocalDate.now();

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_debit_date")
    private LocalDate nextDebitDate;

    @Column(name = "installments_planned")
    private int installmentsPlanned = 12;

    @Column(name = "installments_done")
    private int installmentsDone = 0;

    @Column(name = "installments_failed")
    private int installmentsFailed = 0;

    @Column(name = "total_invested")
    private double totalInvested = 0.0;

    @Column(name = "total_units")
    private double totalUnits = 0.0;

    /** Timestamp when SIP was paused; null if never paused. */
    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "paused_reason", columnDefinition = "TEXT")
    private String pausedReason;

    /** Timestamp when SIP was cancelled; null if not cancelled. */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Sip() {}

    public Sip(UUID userId, String schemeCode, double amount) {
        this.userId = userId;
        this.schemeCode = schemeCode;
        this.amount = amount;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getSchemeCode() { return schemeCode; }
    public UUID getGoalId() { return goalId; }
    public String getFrequency() { return frequency; }
    public double getAmount() { return amount; }
    public int getDebitDay() { return debitDay; }
    public String getStatus() { return status; }
    public String getUpiMandateId() { return upiMandateId; }
    public String getUpiMandateStatus() { return upiMandateStatus; }
    public String getRazorpaySubscriptionId() { return razorpaySubscriptionId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getNextDebitDate() { return nextDebitDate; }
    public int getInstallmentsPlanned() { return installmentsPlanned; }
    public int getInstallmentsDone() { return installmentsDone; }
    public int getInstallmentsFailed() { return installmentsFailed; }
    public double getTotalInvested() { return totalInvested; }
    public double getTotalUnits() { return totalUnits; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setGoalId(UUID goalId) { this.goalId = goalId; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public void setDebitDay(int day) { this.debitDay = day; }
    public void setStatus(String status) { this.status = status; }
    public void setUpiMandateId(String upiMandateId) { this.upiMandateId = upiMandateId; }
    public void setUpiMandateStatus(String status) { this.upiMandateStatus = status; }
    public void setRazorpaySubscriptionId(String subId) { this.razorpaySubscriptionId = subId; }

    public Instant getPausedAt() { return pausedAt; }
    public void setPausedAt(Instant pausedAt) { this.pausedAt = pausedAt; }

    public String getPausedReason() { return pausedReason; }
    public void setPausedReason(String pausedReason) { this.pausedReason = pausedReason; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledReason() { return cancelledReason; }
    public void setCancelledReason(String cancelledReason) { this.cancelledReason = cancelledReason; }
    public void setStartDate(LocalDate date) { this.startDate = date; }
    public void setEndDate(LocalDate date) { this.endDate = date; }
    public void setNextDebitDate(LocalDate date) { this.nextDebitDate = date; }
    public void setInstallmentsPlanned(int val) { this.installmentsPlanned = val; }
    public void setInstallmentsDone(int val) { this.installmentsDone = val; }
    public void setInstallmentsFailed(int val) { this.installmentsFailed = val; }
    public void setTotalInvested(double val) { this.totalInvested = val; }
    public void setTotalUnits(double val) { this.totalUnits = val; }
    public void setUpdatedAt(Instant val) { this.updatedAt = val; }
}

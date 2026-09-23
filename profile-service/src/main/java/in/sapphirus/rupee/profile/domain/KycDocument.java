package in.sapphirus.rupee.profile.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "kyc_documents", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "attempt_number"})
})
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String status = "INITIATED";

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "pan_encrypted", columnDefinition = "TEXT")
    private String panEncrypted;

    @Column(name = "pan_last4", length = 4)
    private String panLast4;

    @Column(name = "pan_name", length = 200)
    private String panName;

    @Column(name = "pan_dob")
    private LocalDate panDob;

    @Column(name = "pan_verified_at")
    private Instant panVerifiedAt;

    @Column(name = "aadhaar_last4", length = 4)
    private String aadhaarLast4;

    @Column(name = "aadhaar_name", length = 200)
    private String aadhaarName;

    @Column(name = "aadhaar_dob")
    private LocalDate aadhaarDob;

    @Column(name = "aadhaar_gender", length = 10)
    private String aadhaarGender;

    @Column(name = "aadhaar_address", columnDefinition = "JSONB")
    private String aadhaarAddress;

    @Column(name = "aadhaar_verified_at")
    private Instant aadhaarVerifiedAt;

    @Column(name = "digilocker_state", length = 100)
    private String digilockerState;

    @Column(name = "digilocker_code", columnDefinition = "TEXT")
    private String digilockerCode;

    @Column(name = "digilocker_access_token", columnDefinition = "TEXT")
    private String digilockerAccessToken;

    @Column(name = "digilocker_ref_id", length = 100)
    private String digilockerRefId;

    @Column(name = "digilocker_completed_at")
    private Instant digilockerCompletedAt;

    @Column(name = "pan_document_s3_key", columnDefinition = "TEXT")
    private String panDocumentS3Key;

    @Column(name = "pan_document_s3_bucket", length = 100)
    private String panDocumentS3Bucket;

    @Column(name = "selfie_s3_key", columnDefinition = "TEXT")
    private String selfieS3Key;

    @Column(name = "selfie_s3_bucket", length = 100)
    private String selfieS3Bucket;

    @Column(name = "aadhaar_xml_s3_key", columnDefinition = "TEXT")
    private String aadhaarXmlS3Key;

    @Column(name = "video_kyc_provider", length = 50)
    private String videoKycProvider;

    @Column(name = "video_kyc_ref_id", length = 100)
    private String videoKycRefId;

    @Column(name = "video_kyc_url", columnDefinition = "TEXT")
    private String videoKycUrl;

    @Column(name = "video_kyc_status", length = 30)
    private String videoKycStatus;

    @Column(name = "video_kyc_score")
    private Double videoKycScore;

    @Column(name = "video_kyc_completed_at")
    private Instant videoKycCompletedAt;

    @Column(name = "name_match_score")
    private Double nameMatchScore;

    @Column(name = "name_match_passed")
    private Boolean nameMatchPassed;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", length = 50)
    private String rejectionReason;

    @Column(name = "rejection_note", columnDefinition = "TEXT")
    private String rejectionNote;

    @Column(name = "reviewer_internal_note", columnDefinition = "TEXT")
    private String reviewerInternalNote;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_id", length = 200)
    private String deviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected KycDocument() {}

    public KycDocument(UUID userId, int attemptNumber) {
        this.userId = userId;
        this.attemptNumber = attemptNumber;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getStatus() { return status; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getPanEncrypted() { return panEncrypted; }
    public String getPanLast4() { return panLast4; }
    public String getPanName() { return panName; }
    public LocalDate getPanDob() { return panDob; }
    public Instant getPanVerifiedAt() { return panVerifiedAt; }
    public String getAadhaarLast4() { return aadhaarLast4; }
    public String getAadhaarName() { return aadhaarName; }
    public LocalDate getAadhaarDob() { return aadhaarDob; }
    public String getAadhaarGender() { return aadhaarGender; }
    public String getAadhaarAddress() { return aadhaarAddress; }
    public Instant getAadhaarVerifiedAt() { return aadhaarVerifiedAt; }
    public String getDigilockerState() { return digilockerState; }
    public String getDigilockerCode() { return digilockerCode; }
    public String getDigilockerAccessToken() { return digilockerAccessToken; }
    public String getDigilockerRefId() { return digilockerRefId; }
    public Instant getDigilockerCompletedAt() { return digilockerCompletedAt; }
    public String getPanDocumentS3Key() { return panDocumentS3Key; }
    public String getPanDocumentS3Bucket() { return panDocumentS3Bucket; }
    public String getSelfieS3Key() { return selfieS3Key; }
    public String getSelfieS3Bucket() { return selfieS3Bucket; }
    public String getAadhaarXmlS3Key() { return aadhaarXmlS3Key; }
    public String getVideoKycProvider() { return videoKycProvider; }
    public String getVideoKycRefId() { return videoKycRefId; }
    public String getVideoKycUrl() { return videoKycUrl; }
    public String getVideoKycStatus() { return videoKycStatus; }
    public Double getVideoKycScore() { return videoKycScore; }
    public Instant getVideoKycCompletedAt() { return videoKycCompletedAt; }
    public Double getNameMatchScore() { return nameMatchScore; }
    public Boolean getNameMatchPassed() { return nameMatchPassed; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public String getRejectionNote() { return rejectionNote; }
    public String getReviewerInternalNote() { return reviewerInternalNote; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getRejectedAt() { return rejectedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getDeviceId() { return deviceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setPanEncrypted(String panEncrypted) { this.panEncrypted = panEncrypted; }
    public void setPanLast4(String panLast4) { this.panLast4 = panLast4; }
    public void setPanName(String panName) { this.panName = panName; }
    public void setPanDob(LocalDate panDob) { this.panDob = panDob; }
    public void setPanVerifiedAt(Instant panVerifiedAt) { this.panVerifiedAt = panVerifiedAt; }
    public void setAadhaarLast4(String aadhaarLast4) { this.aadhaarLast4 = aadhaarLast4; }
    public void setAadhaarName(String aadhaarName) { this.aadhaarName = aadhaarName; }
    public void setAadhaarDob(LocalDate aadhaarDob) { this.aadhaarDob = aadhaarDob; }
    public void setAadhaarGender(String aadhaarGender) { this.aadhaarGender = aadhaarGender; }
    public void setAadhaarAddress(String aadhaarAddress) { this.aadhaarAddress = aadhaarAddress; }
    public void setAadhaarVerifiedAt(Instant aadhaarVerifiedAt) { this.aadhaarVerifiedAt = aadhaarVerifiedAt; }
    public void setDigilockerState(String digilockerState) { this.digilockerState = digilockerState; }
    public void setDigilockerCode(String digilockerCode) { this.digilockerCode = digilockerCode; }
    public void setDigilockerAccessToken(String token) { this.digilockerAccessToken = token; }
    public void setDigilockerRefId(String refId) { this.digilockerRefId = refId; }
    public void setDigilockerCompletedAt(Instant completedAt) { this.digilockerCompletedAt = completedAt; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setRejectionReason(String reason) { this.rejectionReason = reason; }
    public void setRejectionNote(String note) { this.rejectionNote = note; }
    public void setReviewerInternalNote(String note) { this.reviewerInternalNote = note; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

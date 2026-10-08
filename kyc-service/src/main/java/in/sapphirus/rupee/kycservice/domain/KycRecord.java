package in.sapphirus.rupee.kycservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(name = "aadhaar_last_four", length = 4, nullable = false)
    private String aadhaarLastFour;

    @Column(name = "encrypted_pan", columnDefinition = "TEXT", nullable = false)
    private String encryptedPan;

    @Column(name = "pan_iv", columnDefinition = "TEXT", nullable = false)
    private String panIv;

    @Column(name = "pan_auth_tag", columnDefinition = "TEXT", nullable = false)
    private String panAuthTag;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "verified_name", nullable = false)
    private String verifiedName;

    @Column(name = "name_match_percentage")
    private double nameMatchPercentage;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
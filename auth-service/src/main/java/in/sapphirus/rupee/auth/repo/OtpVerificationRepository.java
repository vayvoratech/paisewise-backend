package in.sapphirus.rupee.auth.repo;

import in.sapphirus.rupee.auth.domain.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {
    Optional<OtpVerification> findTopByPhoneAndPurposeOrderByCreatedAtDesc(String phone, String purpose);
}

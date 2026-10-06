package in.sapphirus.rupee.kycservice.repo;

import in.sapphirus.rupee.kycservice.domain.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycRepository extends JpaRepository<KycRecord, Long> {
    Optional<KycRecord> findByUserId(Long userId);
}
package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.MfInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfInvestmentRepository extends JpaRepository<MfInvestment, UUID> {
    List<MfInvestment> findByUserIdOrderByTransactionDateDesc(UUID userId);
    List<MfInvestment> findByUserIdAndStatusOrderByTransactionDateDesc(UUID userId, String status);
    Optional<MfInvestment> findByBseOrderId(String bseOrderId);
    List<MfInvestment> findByUserIdAndSchemeCode(UUID userId, String schemeCode);
    List<MfInvestment> findBySipId(UUID sipId);
}


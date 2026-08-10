package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.MfInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MfInvestmentRepository extends JpaRepository<MfInvestment, UUID> {
    List<MfInvestment> findByUserIdOrderByTransactionDateDesc(UUID userId);
}

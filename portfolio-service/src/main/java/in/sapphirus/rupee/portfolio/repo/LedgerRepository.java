package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    List<Ledger> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    List<Ledger> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Ledger> findByUserIdOrderByIdDesc(UUID userId);
    Optional<Ledger> findTopByUserIdOrderByIdDesc(UUID userId);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.type = 'CREDIT' THEN l.amount ELSE -l.amount END), 0.0) FROM Ledger l WHERE l.userId = :userId")
    Double calculateBalance(@Param("userId") UUID userId);
}

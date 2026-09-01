package in.sapphirus.rupee.practice.repo;

import in.sapphirus.rupee.practice.domain.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {

    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT p FROM PriceAlert p WHERE p.symbol = :symbol AND p.status = 'ACTIVE'")
    List<PriceAlert> findActiveAlertsForSymbol(@Param("symbol") String symbol);
}
